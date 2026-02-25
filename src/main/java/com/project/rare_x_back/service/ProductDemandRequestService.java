package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.ProductDemandRequestDto;
import com.project.rare_x_back.dto.response.ProductDemandRequestResponseDto;
import com.project.rare_x_back.entity.ProductDemandRequest;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.ProductDemandRequestRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductDemandRequestService {

    private final ProductDemandRequestRepository demandRequestRepository;
    private final UserRepository userRepository;
    private final S3ImageService s3ImageService;

    // 유저가 상품 등록 요청
    @Transactional
    public ProductDemandRequestResponseDto createDemandRequest(Long userId, ProductDemandRequestDto dto, MultipartFile image ) {

        // 유저 조회
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이미지 필수 체크
        if (image == null || image.isEmpty()) {
            throw new CustomException(
                    ErrorCode.BAD_REQUEST,
                    "상품 이미지는 필수입니다."
            );
        }

        // 이미지 S3 업로드
        String imageUrl = s3ImageService.uploadProductRequestImage(image);

        // Entity 생성
        ProductDemandRequest demand =
                ProductDemandRequest.builder()
                        .user(user)
                        .productName(dto.getProductName())
                        .brandName(dto.getBrandName())
                        .retailPrice(dto.getRetailPrice())
                        .imageUrl(imageUrl)
                        .description(dto.getDescription())
                        .build();
        //  저장
        demandRequestRepository.save(demand);

        return ProductDemandRequestResponseDto.builder()
                .userId(demand.getUser().getUserId())
                .demandId(demand.getDemandId())
                .productName(demand.getProductName())
                .brandName(demand.getBrandName())
                .retailPrice(demand.getRetailPrice())
                .description(demand.getDescription())
                .imageUrl(demand.getImageUrl())
                .createdAt(demand.getCreatedAt())
                .build();
    }
}

