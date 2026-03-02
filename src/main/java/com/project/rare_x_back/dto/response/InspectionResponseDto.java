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
    private Long inspectorId;
    private String inspectorName;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime inspectedAt;
    private String orderCurrentStatus;  // 주문 상태 (ORDER 타입일 때만)

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
        if (inspection.getStorageRequest() != null) {
            setSellerInfo(builder, inspection.getStorageRequest().getUser());
            setProductInfo(builder, inspection.getStorageRequest().getProduct());
        }

        // 반송(RELEASE)인 경우 - 고객 요청
        else if (inspection.getStorageItem() != null) {
            setSellerInfo(builder, inspection.getStorageItem().getUser());
            setProductInfo(builder, inspection.getStorageItem().getProduct());
        }

        // 주문 검수(ORDER)인 경우
        else if (inspection.getOrder() != null) {
            Order order = inspection.getOrder();
            if (order.getCurrentStatus() != null) {
                builder.orderCurrentStatus(order.getCurrentStatus().name());
            }
            setSellerInfo(builder, order.getSeller());
            setProductInfo(builder, order.getProduct());
        }

        // 검수 담당자 정보
        setInspectorInfo(builder, inspection.getUser());

        return builder.build();
    }

    /**
     * 복잡도 낮추기 위한 분리 메서드
     * @setInspectorInfo : 검수 담당자 정보 조회
     * @setSellerInfo : 판매자 정보 조회
     * @setProductInfo : 상품 정보 조회
     * */
    private static void setInspectorInfo(InspectionResponseDtoBuilder builder, User inspector) {
        if (inspector != null) {
            builder.inspectorId(inspector.getUserId())
                    .inspectorName(inspector.getName());
        }
    }
    private static void setSellerInfo(InspectionResponseDtoBuilder builder, User user) {
        if (user != null) {
            builder.sellerId(user.getUserId())
                    .sellerName(user.getName());
        }
    }
    private static void setProductInfo(InspectionResponseDtoBuilder builder, Product product) {
        if (product == null) return;
        builder.productId(product.getProductId())
                .productName(product.getProductName());
        if (product.getBrand() != null) {
            builder.brandName(product.getBrand().getBrandName());
        }
    }
}
