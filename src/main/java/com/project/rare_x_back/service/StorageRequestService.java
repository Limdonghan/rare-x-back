package com.project.rare_x_back.service;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.dto.request.StorageRequestCreateDto;
import com.project.rare_x_back.dto.response.StorageRequestResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.enums.StorageRequestStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageRequestService {

    private final StorageRequestRepository storageRequestRepository;
    private final InspectionRepository inspectionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PaymentService paymentService;
    private final StorageDepositRepository storageDepositRepository;

    @Value("${inspection-center.address}")
    private String inspectionCenterAddress;

    @Value("${inspection-center.zipcode}")
    private String inspectionCenterZipcode;

    // 보관 판매 신청
    @Transactional
    public StorageRequestResponseDto createStorageRequest(String userEmail, StorageRequestCreateDto request) {

        // 1. 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 빌링키 등록 여부 확인
        if (!billingKeyRepository.existsByUser_UserId(user.getUserId())) {
            throw new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND,
                    "보관 판매 신청을 위해 카드 등록이 필요합니다.");
        }

        // 3. 상품 조회
        Product product = productRepository.findByProductIdAndIsDeletedFalse(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 4. 보관 신청 생성
        StorageRequest storageRequest = StorageRequest.builder()
                .user(user)
                .product(product)
                .status(StorageRequestStatus.PENDING)
                .build();
        StorageRequest saved = storageRequestRepository.save(storageRequest);

        // 5. 보증금 결제
        StorageDeposit deposit = StorageDeposit.builder()
                .storageRequest(saved)
                .user(user)
                .build();
        storageDepositRepository.save(deposit);

        try {
            String tossPaymentKey = paymentService.payStorageFeeWithBillingKey(
                    user.getUserId(), FeeCalculator.STORAGE_DEPOSIT);
            deposit.markSuccess(tossPaymentKey);
        } catch (Exception e) {
            deposit.markFailed();
            storageRequestRepository.delete(saved);  // 결제 실패 시 신청도 롤백
            throw new CustomException(ErrorCode.PAYMENT_FAILED, "보증금 결제 실패: " + e.getMessage());
        }

        // 6. 응답 반환
        return StorageRequestResponseDto.from(saved, inspectionCenterAddress, inspectionCenterZipcode);
    }

    // 보관 신청 목록 조회 (전체 또는 상태별 필터링)
    public List<StorageRequestResponseDto> getMyStorageRequests(String userEmail, StorageRequestStatus status) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<StorageRequest> requests;

        if (status != null) {
            requests = storageRequestRepository.findByUserIdAndStatus(user.getUserId(), status);
        } else {
            requests = storageRequestRepository.findByUserId(user.getUserId());
        }

        return requests.stream()
                .map(sr -> StorageRequestResponseDto.from(sr, inspectionCenterAddress, inspectionCenterZipcode))
                .collect(Collectors.toList());
    }

    // 발송 대기 목록 조회 (PENDING 상태)
    public List<StorageRequestResponseDto> getPendingStorageRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<StorageRequest> requests = storageRequestRepository
                .findByUserIdAndStatus(user.getUserId(), StorageRequestStatus.PENDING);

        return requests.stream()
                .map(sr -> StorageRequestResponseDto.from(sr, inspectionCenterAddress, inspectionCenterZipcode))
                .collect(Collectors.toList());
    }

    // 발송 처리
    @Transactional
    public StorageRequestResponseDto shipToWarehouse(String userEmail, Long storageRequestId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 보관 신청 조회
        StorageRequest storageRequest = storageRequestRepository.findByIdWithDetails(storageRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "보관 신청을 찾을 수 없습니다."));

        // 2. 본인 확인
        if (!storageRequest.getUser().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 보관 신청만 발송 처리할 수 있습니다.");
        }

        // 3. 상태 확인 (PENDING만 발송 가능)
        if (storageRequest.getStatus() != StorageRequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "발송 대기 상태에서만 발송 처리가 가능합니다.");
        }

        // 4. StorageRequest 상태 변경
        storageRequest.updateStatus(StorageRequestStatus.SHIPPED_TO_WAREHOUSE);

        // 5. Inspection 생성
        Inspection inspection = Inspection.builder()
                .storageRequest(storageRequest)
                .type(InspectionType.STORAGE)
                .status(InspectionStatus.SHIPPED_TO_WAREHOUSE)
                .build();

        inspectionRepository.save(inspection);

        return StorageRequestResponseDto.from(storageRequest, inspectionCenterAddress, inspectionCenterZipcode);
    }
}