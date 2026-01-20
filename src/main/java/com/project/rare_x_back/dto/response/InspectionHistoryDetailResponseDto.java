package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.InspectionChecklist;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.StorageRequest;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InspectionHistoryDetailResponseDto {

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

    // 체크리스트 정보
    private InspectionChecklistResponseDto checklist;

    public static InspectionHistoryDetailResponseDto from(Inspection inspection, InspectionChecklist checklist) {
        var builder = InspectionHistoryDetailResponseDto.builder()
                .inspectionId(inspection.getInspectionId())
                .type(inspection.getType())
                .status(inspection.getStatus())
                .failReason(inspection.getFailReason())
                .createdAt(inspection.getCreatedAt())
                .startedAt(inspection.getStartedAt())
                .inspectedAt(inspection.getInspectedAt());

        // 검수 담당자 정보
        User inspector = inspection.getUser();
        if (inspector != null) {
            builder.inspectorId(inspector.getUserId())
                    .inspectorName(inspector.getName());
        }

        // TODO: ORDER 타입 검수 시 Order 엔티티 연동 후 상품/판매자 정보 추가
        // 보관 검수인 경우
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

                // 카테고리 정보
                if (product.getCategory() != null) {
                    builder.categoryId(product.getCategory().getCategoryId())
                            .categoryName(product.getCategory().getCategoryName());
                }
            }
        }

        // 체크리스트 정보
        if (checklist != null) {
            builder.checklist(InspectionChecklistResponseDto.from(checklist));
        }

        return builder.build();
    }
}