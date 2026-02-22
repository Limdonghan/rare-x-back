package com.project.rare_x_back.service;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.dto.request.StorageRequestCreateDto;
import com.project.rare_x_back.dto.response.StorageRequestResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.enums.StoragePaymentStatus;
import com.project.rare_x_back.enums.StorageRequestStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
    private final SearchService searchService;

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

        // 4. 보증금 결제 먼저 시도 (실패하면 신청 생성 없이 바로 에러)
        int quantity = request.getQuantity();
        int totalDeposit = FeeCalculator.STORAGE_DEPOSIT * quantity;

        String tossPaymentKey;
        try {
            tossPaymentKey = paymentService.payStorageFeeWithBillingKey(
                    user.getUserId(), totalDeposit);
        } catch (Exception e) {
            log.error("보관 보증금 결제 실패: userId={}, amount={}", user.getUserId(), totalDeposit, e);
            throw new CustomException(ErrorCode.PAYMENT_FAILED, "보증금 결제에 실패했습니다. 카드 정보를 확인해주세요.");
        }

        // 5. 결제 성공 후 신청 생성 + 건별 보증금 기록
        List<StorageRequest> savedRequests = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            StorageRequest storageRequest = StorageRequest.builder()
                    .user(user)
                    .product(product)
                    .status(StorageRequestStatus.PENDING)
                    .build();
            savedRequests.add(storageRequestRepository.save(storageRequest));

            StorageDeposit deposit = StorageDeposit.builder()
                    .storageRequest(storageRequest)
                    .user(user)
                    .build();
            deposit.markSuccess(tossPaymentKey);
            storageDepositRepository.save(deposit);
        }

        // 6. 첫 번째 신청 기준 응답 반환
        return StorageRequestResponseDto.from(savedRequests.get(0), inspectionCenterAddress, inspectionCenterZipcode);
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
        searchService.indexInspection(inspection);

        return StorageRequestResponseDto.from(storageRequest, inspectionCenterAddress, inspectionCenterZipcode);
    }

    // 보관 신청 취소 (발송 전)
    @Transactional
    public void cancelStorageRequest(String userEmail, Long storageRequestId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 보관 신청 조회
        StorageRequest storageRequest = storageRequestRepository.findByIdWithDetails(storageRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "보관 신청을 찾을 수 없습니다."));

        // 2. 본인 확인
        if (!storageRequest.getUser().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 보관 신청만 취소할 수 있습니다.");
        }

        // 3. 상태 확인 (PENDING만 취소 가능)
        if (storageRequest.getStatus() != StorageRequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "발송 대기 상태에서만 보관 신청 취소가 가능합니다.");
        }

        // 4. 결제(보증금) 내역 확인 및 환불 처리
        StorageDeposit deposit = storageDepositRepository.findByStorageRequest_StorageRequestId(storageRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "보관 보증금 결제 내역을 찾을 수 없습니다."));

        // 이미 성공 상태인 보증금만 결제 취소 API 호출
//        if (deposit.getStatus() == StoragePaymentStatus.SUCCESS && deposit.getTossPaymentKey() != null) {
//            paymentService.cancelStorageDeposit(deposit, deposit.getAmount(), "사용자 요청에 의한 보관 신청 취소");
//
//            // 보증금 상태를 취소로 변경
//            deposit.markCanceled();

        StoragePaymentStatus paymentStatus = deposit.getStatus();

        // 결제 상태에 따른 보증금 처리
        if (deposit.getTossPaymentKey() == null) {
            // 결제 키가 없는 상태에서 취소 요청이 들어온 경우: 예외 상황이므로 로그만 남김
            log.warn("보관 보증금 결제 키가 없는 상태에서 취소가 요청되었습니다. storageRequestId={}, paymentStatus={}",
                    storageRequestId, paymentStatus);

        } else if (paymentStatus == StoragePaymentStatus.SUCCESS) {
            try {
                // 이미 성공 상태인 보증금만 결제 취소 API 호출
                paymentService.cancelStorageDeposit(deposit, deposit.getAmount(), "사용자 요청에 의한 보관 신청 취소");
                // 보증금 상태를 취소로 변경
                deposit.markCanceled();
            } catch (Exception e) {
                log.error("보증금 환불(결제 취소) 중 오류가 발생하여 보관 신청 취소를 중단(Rollback)합니다. storageRequestId={}", storageRequestId, e);
                throw new CustomException(ErrorCode.PAYMENT_FAILED, "보증금 결제 취소에 실패하여 보관 신청을 취소할 수 없습니다.");
            }

        } else if (paymentStatus == StoragePaymentStatus.PENDING) {
            // 결제가 진행 중인 상태에서의 취소 요청: 현재는 별도 취소 시도는 하지 않고 경고 로그만 남김
            log.warn("보관 보증금 결제가 PENDING 상태인 동안 보관 신청 취소가 요청되었습니다. "
                            + "추가적인 결제 취소 처리 여부를 검토하세요. storageRequestId={}, paymentStatus={}",
                    storageRequestId, paymentStatus);

        } else if (paymentStatus == StoragePaymentStatus.FAILED) {
            // 이미 결제가 실패한 상태: 환불 시도는 필요 없으나 상황을 로그로 남김
            log.info("보관 보증금 결제가 FAILED 상태인 보관 신청에 대해 취소가 요청되었습니다. "
                            + "추가적인 결제 처리 없이 보관 신청만 취소됩니다. storageRequestId={}, paymentStatus={}",
                    storageRequestId, paymentStatus);

        } else {
            // 정의되지 않은/예상하지 못한 상태에 대한 방어적 로깅
            log.warn("예상하지 못한 보관 보증금 결제 상태에서 보관 신청 취소가 요청되었습니다. "
                            + "storageRequestId={}, paymentStatus={}",
                    storageRequestId, paymentStatus);
        }

        // 5. StorageRequest 상태 변경
        storageRequest.updateStatus(StorageRequestStatus.CANCELLED);
    }
}