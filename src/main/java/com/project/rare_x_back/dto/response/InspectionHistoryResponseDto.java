package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InspectionHistoryResponseDto {

    private Long inspectionId;
    private InspectionType type;
    private InspectionStatus status;

    // 상품 정보
    private Long productId;
    private String productName;
    private String brandName;
    private Long categoryId;
    private String categoryName;

    // 판매자 정보
    private Long sellerId;
    private String sellerName;

    // 검수 담당자 정보
    private Long inspectorId;
    private String inspectorName;

    // 검수 정보
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime inspectedAt;

    public static InspectionHistoryResponseDto from(Inspection inspection) {
        var builder = InspectionHistoryResponseDto.builder()
                .inspectionId(inspection.getInspectionId())
                .type(inspection.getType())
                .status(inspection.getStatus())
                .failReason(inspection.getFailReason())
                .createdAt(inspection.getCreatedAt())
                .startedAt(inspection.getStartedAt())
                .inspectedAt(inspection.getInspectedAt());

        // 검수 담당자 정보
        setInspectorInfo(builder,inspection.getUser());

        // 보관 검수인 경우
        if (inspection.getStorageRequest() != null) {
            // 판매자 정보
            setSellerInfo(builder,inspection.getStorageRequest().getUser());

            // 상품 정보
            setProductInfo(builder,inspection.getStorageRequest().getProduct());
        }

        // 주문 검수(ORDER)인 경우
        else if (inspection.getOrder() != null) {
            // 판매자 정보
            setSellerInfo(builder,inspection.getOrder().getSeller());

            // 상품 정보
            setProductInfo(builder,inspection.getOrder().getProduct());
        }

        return builder.build();
    }

    /**
     * 복잡도 낮추기 위한 분리 메서드
     * @setInspectorInfo : 검수 담당자 정보 조회
     * @setSellerInfo : 판매자 정보 조회
     * @setProductInfo : 상품 정보 조회
     * */
    private static void setInspectorInfo(InspectionHistoryResponseDtoBuilder builder, User inspector) {
        if (inspector != null) {
            builder.inspectorId(inspector.getUserId())
                    .inspectorName(inspector.getName());
        }
    }
    private static void setSellerInfo(InspectionHistoryResponseDtoBuilder builder, User seller) {
        if (seller != null) {
            builder.sellerId(seller.getUserId())
                    .sellerName(seller.getName());
        }
    }
    private static void setProductInfo(InspectionHistoryResponseDtoBuilder builder, Product product) {
        if (product == null) return;
        builder.productId(product.getProductId())
                .productName(product.getProductName());
        if (product.getBrand() != null) {
            builder.brandName(product.getBrand().getBrandName());
        }
        if (product.getCategory() != null) {
            builder.categoryId(product.getCategory().getCategoryId())
                    .categoryName(product.getCategory().getCategoryName());
        }
    }
}