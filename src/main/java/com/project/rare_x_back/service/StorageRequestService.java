package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.StorageRequestCreateDto;
import com.project.rare_x_back.dto.response.StorageRequestResponseDto;
import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.StorageRequest;
import com.project.rare_x_back.entity.User;
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

        // 2. 빌링키 등록 여부 확인 (180일 이후 자동결제용)
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

        // 5. 저장
        StorageRequest saved = storageRequestRepository.save(storageRequest);

        // 6. 응답 반환
        return StorageRequestResponseDto.from(saved, inspectionCenterAddress, inspectionCenterZipcode);
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

        // 2. 본인 확인 (백엔드 보안 체크 : URL의 123을 456으로 바꿔서 요청하면, 다른 사람의 보관 신청을 발송 처리 가능)
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