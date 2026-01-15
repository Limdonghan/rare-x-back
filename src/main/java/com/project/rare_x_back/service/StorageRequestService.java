package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.StorageRequestCreateDto;
import com.project.rare_x_back.dto.response.StorageRequestResponseDto;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.StorageRequest;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.StorageRequestStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.ProductRepository;
import com.project.rare_x_back.repository.StorageRequestRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageRequestService {

    private final StorageRequestRepository storageRequestRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Value("${inspection-center.address}")
    private String inspectionCenterAddress;

    @Value("${inspection-center.zipcode}")
    private String inspectionCenterZipcode;

    // 보관 판매 신청
    @Transactional
    public StorageRequestResponseDto createStorageRequest(Long userId, StorageRequestCreateDto request) {

        // 1. 사용자 조회
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 상품 조회
        Product product = productRepository.findByProductIdAndIsDeletedFalse(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. 보관 신청 생성
        StorageRequest storageRequest = StorageRequest.builder()
                .user(user)
                .product(product)
                .status(StorageRequestStatus.PENDING_INSPECTION)
                .build();

        // 4. 저장
        StorageRequest saved = storageRequestRepository.save(storageRequest);

        // 5. 응답 반환
        return StorageRequestResponseDto.from(saved, inspectionCenterAddress, inspectionCenterZipcode);
    }
}