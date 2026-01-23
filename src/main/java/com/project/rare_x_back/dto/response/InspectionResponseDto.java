package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InspectionResponseDto {

    private Long inspectionId;
    private InspectionType type;
    private InspectionStatus status;

    // 상품 정보
    private Long productId;
    private String productName;
    private String brandName;

    // 판매자 정보
    private Long sellerId;
    private String sellerName;

    // 검수 정보
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime inspectedAt;

    // Inspection 엔티티를 ResponseDto로 변환
    public static InspectionResponseDto from(Inspection inspection) {
        var builder = InspectionResponseDto.builder()   // 장황한 빌더 타입 선언을 생략하여 코드 가독성을 높임(var builder)
                .inspectionId(inspection.getInspectionId())
                .type(inspection.getType())
                .status(inspection.getStatus())
                .failReason(inspection.getFailReason())
                .createdAt(inspection.getCreatedAt())
                .inspectedAt(inspection.getInspectedAt());

        // 보관 검수(STORAGE)인 경우
        StorageRequest storageRequest = inspection.getStorageRequest();
        if (storageRequest != null) {
            // 판매자 정보
            User seller = storageRequest.getUser();
            if (seller != null) {
                builder.sellerId(seller.getUserId())
                        .sellerName(seller.getName());
            }

            // 상품 정보
            Product product = storageRequest.getProduct();
            if (product != null) {
                builder.productId(product.getProductId())
                        .productName(product.getProductName());

                // 브랜드 정보
                if (product.getBrand() != null) {
                    builder.brandName(product.getBrand().getBrandName());
                }
            }
        }

        // 주문 검수(ORDER)인 경우
        Order order = inspection.getOrder();
        if (order != null) {
            // 판매자 정보
            User seller = order.getSeller();
            if (seller != null) {
                builder.sellerId(seller.getUserId())
                        .sellerName(seller.getName());
            }

            // 상품 정보
            Product product = order.getProduct();
            if (product != null) {
                builder.productId(product.getProductId())
                        .productName(product.getProductName());

                if (product.getBrand() != null) {
                    builder.brandName(product.getBrand().getBrandName());
                }
            }
        }

        return builder.build();
    }
}
