package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;
import org.typesense.model.SearchResultHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final Client typesenseClient;

    // ==================== 상품 검색 ====================

    /**
     * 상품 검색 (회원용 + 관리자용 공통)
     */
    public SearchResultDto<ProductSearchResponseDto> searchProducts(String keyword, Pageable pageable) {
        try {
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy("product_name,brand_name,category_name")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections("products")
                    .documents()
                    .search(params);

            return convertToProductDto(result, pageable);

        } catch (Exception e) {
            log.error("상품 검색 실패: {}", e.getMessage());
            throw new RuntimeException("검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회원 검색 (관리자용)
     */
    public SearchResultDto<UserSearchResponseDto> searchUsers(String keyword, Pageable pageable) {
        try {
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy("email,name")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections("users")
                    .documents()
                    .search(params);

            return convertToUserDto(result, pageable);

        } catch (Exception e) {
            log.error("회원 검색 실패: {}", e.getMessage());
            throw new RuntimeException("검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 주문 검색 (관리자용)
     */
    public SearchResultDto<OrderSearchResponseDto> searchOrders(String keyword, Pageable pageable) {
        try {
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy("product_name,buyer_name,seller_name")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections("orders")
                    .documents()
                    .search(params);

            return convertToOrderDto(result, pageable);

        } catch (Exception e) {
            log.error("주문 검색 실패: {}", e.getMessage());
            throw new RuntimeException("검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 검색 (관리자용)
     */
    public SearchResultDto<InspectionSearchResponseDto> searchInspections(String keyword, Pageable pageable) {
        try {
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy("product_name,seller_name,inspector_name")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections("inspections")
                    .documents()
                    .search(params);

            return convertToInspectionDto(result, pageable);

        } catch (Exception e) {
            log.error("검수 검색 실패: {}", e.getMessage());
            throw new RuntimeException("검색 중 오류가 발생했습니다.", e);
        }
    }

    // ==================== 인덱싱 메서드 ====================

    /**
     * 상품 인덱싱
     */
    public void indexProduct(Product product) {
        try {
            Map<String, Object> document = new HashMap<>();
            document.put("id", String.valueOf(product.getProductId()));
            document.put("product_id", product.getProductId());
            document.put("product_name", product.getProductName());
            document.put("brand_name", product.getBrand() != null ? product.getBrand().getBrandName() : "");
            document.put("category_name", product.getCategory() != null ? product.getCategory().getCategoryName() : "");
            document.put("product_description", product.getProductDescription());
            document.put("retail_price", product.getRetailPrice());

            typesenseClient.collections("products")
                    .documents()
                    .upsert(document);

            log.info("상품 인덱싱 완료: {}", product.getProductId());

        } catch (Exception e) {
            log.error("상품 인덱싱 실패: {}", e.getMessage());
        }
    }

    /**
     * 회원 인덱싱
     */
    public void indexUser(User user) {
        try {
            Map<String, Object> document = new HashMap<>();
            document.put("id", String.valueOf(user.getUserId()));
            document.put("user_id", user.getUserId());
            document.put("email", user.getEmail());
            document.put("name", user.getName());
            document.put("role", user.getRole().name());
            document.put("status", user.getStatus().name());

            typesenseClient.collections("users")
                    .documents()
                    .upsert(document);

            log.info("회원 인덱싱 완료: {}", user.getUserId());

        } catch (Exception e) {
            log.error("회원 인덱싱 실패: {}", e.getMessage());
        }
    }

    /**
     * 주문 인덱싱
     */
    public void indexOrder(Order order) {
        try {
            Map<String, Object> document = new HashMap<>();
            document.put("id", String.valueOf(order.getOrderId()));
            document.put("order_id", order.getOrderId());
            document.put("buyer_name", order.getBuyer() != null ? order.getBuyer().getName() : "");
            document.put("seller_name", order.getSeller() != null ? order.getSeller().getName() : "");
            document.put("product_name", order.getProduct() != null ? order.getProduct().getProductName() : "");
            document.put("price", order.getPrice());
            document.put("current_status", order.getCurrentStatus() != null ? order.getCurrentStatus().name() : "");

            typesenseClient.collections("orders")
                    .documents()
                    .upsert(document);

            log.info("주문 인덱싱 완료: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("주문 인덱싱 실패: {}", e.getMessage());
        }
    }

    /**
     * 검수 인덱싱
     */
    public void indexInspection(Inspection inspection) {
        try {
            String productName = "";
            String sellerName = "";

            if (inspection.getOrder() != null) {
                productName = inspection.getOrder().getProduct() != null
                        ? inspection.getOrder().getProduct().getProductName() : "";
                sellerName = inspection.getOrder().getSeller() != null
                        ? inspection.getOrder().getSeller().getName() : "";
            } else if (inspection.getStorageRequest() != null) {
                productName = inspection.getStorageRequest().getProduct() != null
                        ? inspection.getStorageRequest().getProduct().getProductName() : "";
                sellerName = inspection.getStorageRequest().getUser() != null
                        ? inspection.getStorageRequest().getUser().getName() : "";
            }

            Map<String, Object> document = new HashMap<>();
            document.put("id", String.valueOf(inspection.getInspectionId()));
            document.put("inspection_id", inspection.getInspectionId());
            document.put("product_name", productName);
            document.put("seller_name", sellerName);
            document.put("inspector_name", inspection.getUser() != null ? inspection.getUser().getName() : "");
            document.put("type", inspection.getType().name());
            document.put("status", inspection.getStatus().name());

            typesenseClient.collections("inspections")
                    .documents()
                    .upsert(document);

            log.info("검수 인덱싱 완료: {}", inspection.getInspectionId());

        } catch (Exception e) {
            log.error("검수 인덱싱 실패: {}", e.getMessage());
        }
    }

    // ==================== 삭제 메서드 ====================

    public void deleteProduct(Long productId) {
        try {
            typesenseClient.collections("products")
                    .documents(String.valueOf(productId))
                    .delete();
            log.info("상품 인덱스 삭제 완료: {}", productId);
        } catch (Exception e) {
            log.error("상품 인덱스 삭제 실패: {}", e.getMessage());
        }
    }

    public void deleteUser(Long userId) {
        try {
            typesenseClient.collections("users")
                    .documents(String.valueOf(userId))
                    .delete();
            log.info("회원 인덱스 삭제 완료: {}", userId);
        } catch (Exception e) {
            log.error("회원 인덱스 삭제 실패: {}", e.getMessage());
        }
    }

    public void deleteOrder(Long orderId) {
        try {
            typesenseClient.collections("orders")
                    .documents(String.valueOf(orderId))
                    .delete();
            log.info("주문 인덱스 삭제 완료: {}", orderId);
        } catch (Exception e) {
            log.error("주문 인덱스 삭제 실패: {}", e.getMessage());
        }
    }

    public void deleteInspection(Long inspectionId) {
        try {
            typesenseClient.collections("inspections")
                    .documents(String.valueOf(inspectionId))
                    .delete();
            log.info("검수 인덱스 삭제 완료: {}", inspectionId);
        } catch (Exception e) {
            log.error("검수 인덱스 삭제 실패: {}", e.getMessage());
        }
    }

    // ==================== 전체 동기화 메서드 ====================

    public int syncAllProducts(List<Product> products) {
        int count = 0;
        for (Product product : products) {
            try {
                indexProduct(product);
                count++;
            } catch (Exception e) {
                log.error("상품 인덱싱 실패 - productId: {}", product.getProductId());
            }
        }
        log.info("전체 상품 동기화 완료: {}건", count);
        return count;
    }

    public int syncAllUsers(List<User> users) {
        int count = 0;
        for (User user : users) {
            try {
                indexUser(user);
                count++;
            } catch (Exception e) {
                log.error("회원 인덱싱 실패 - userId: {}", user.getUserId());
            }
        }
        log.info("전체 회원 동기화 완료: {}건", count);
        return count;
    }

    public int syncAllOrders(List<Order> orders) {
        int count = 0;
        for (Order order : orders) {
            try {
                indexOrder(order);
                count++;
            } catch (Exception e) {
                log.error("주문 인덱싱 실패 - orderId: {}", order.getOrderId());
            }
        }
        log.info("전체 주문 동기화 완료: {}건", count);
        return count;
    }

    public int syncAllInspections(List<Inspection> inspections) {
        int count = 0;
        for (Inspection inspection : inspections) {
            try {
                indexInspection(inspection);
                count++;
            } catch (Exception e) {
                log.error("검수 인덱싱 실패 - inspectionId: {}", inspection.getInspectionId());
            }
        }
        log.info("전체 검수 동기화 완료: {}건", count);
        return count;
    }

    // ==================== DTO 변환 메서드 ====================

    private SearchResultDto<ProductSearchResponseDto> convertToProductDto(SearchResult result, Pageable pageable) {
        List<ProductSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                Map<String, Object> doc = hit.getDocument();
                items.add(ProductSearchResponseDto.builder()
                        .productId(((Number) doc.get("product_id")).longValue())
                        .productName((String) doc.get("product_name"))
                        .brandName((String) doc.get("brand_name"))
                        .categoryName((String) doc.get("category_name"))
                        .productDescription((String) doc.get("product_description"))
                        .retailPrice(((Number) doc.get("retail_price")).intValue())
                        .build());
            }
        }

        return SearchResultDto.<ProductSearchResponseDto>builder()
                .totalCount(result.getFound())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .items(items)
                .build();
    }

    private SearchResultDto<UserSearchResponseDto> convertToUserDto(SearchResult result, Pageable pageable) {
        List<UserSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                Map<String, Object> doc = hit.getDocument();
                items.add(UserSearchResponseDto.builder()
                        .userId(((Number) doc.get("user_id")).longValue())
                        .email((String) doc.get("email"))
                        .name((String) doc.get("name"))
                        .role((String) doc.get("role"))
                        .status((String) doc.get("status"))
                        .build());
            }
        }

        return SearchResultDto.<UserSearchResponseDto>builder()
                .totalCount(result.getFound())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .items(items)
                .build();
    }

    private SearchResultDto<OrderSearchResponseDto> convertToOrderDto(SearchResult result, Pageable pageable) {
        List<OrderSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                Map<String, Object> doc = hit.getDocument();
                items.add(OrderSearchResponseDto.builder()
                        .orderId(((Number) doc.get("order_id")).longValue())
                        .buyerName((String) doc.get("buyer_name"))
                        .sellerName((String) doc.get("seller_name"))
                        .productName((String) doc.get("product_name"))
                        .price(((Number) doc.get("price")).intValue())
                        .currentStatus((String) doc.get("current_status"))
                        .build());
            }
        }

        return SearchResultDto.<OrderSearchResponseDto>builder()
                .totalCount(result.getFound())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .items(items)
                .build();
    }

    private SearchResultDto<InspectionSearchResponseDto> convertToInspectionDto(SearchResult result, Pageable pageable) {
        List<InspectionSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                Map<String, Object> doc = hit.getDocument();
                items.add(InspectionSearchResponseDto.builder()
                        .inspectionId(((Number) doc.get("inspection_id")).longValue())
                        .productName((String) doc.get("product_name"))
                        .sellerName((String) doc.get("seller_name"))
                        .inspectorName((String) doc.get("inspector_name"))
                        .type((String) doc.get("type"))
                        .status((String) doc.get("status"))
                        .build());
            }
        }

        return SearchResultDto.<InspectionSearchResponseDto>builder()
                .totalCount(result.getFound())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .items(items)
                .build();
    }
}