package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String POPULAR_KEYWORD_KEY = "search:popular"; // Redis key
    private final Executor taskExecutor; // Executor 주입
    private final Client typesenseClient;
    @Value("${app.service-start-date}")
    private String serviceStartDate;

    // ==================== [상수 선언] ====================
    private static final String COL_PRODUCTS = "products";
    private static final String COL_USERS = "users";
    private static final String COL_ORDERS = "orders";
    private static final String COL_INSPECTIONS = "inspections";

    private static final String SORT_CREATED_AT_DESC = "created_at:desc";
    private static final String FILTER_IS_DELETED_FALSE = "is_deleted:false";

    private static final String FIELD_ID = "id";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_IS_DELETED = "is_deleted";
    private static final String FIELD_PRODUCT_ID = "product_id";
    private static final String FIELD_PRODUCT_NAME = "product_name";
    private static final String FIELD_BRAND_NAME = "brand_name";
    private static final String FIELD_CATEGORY_NAME = "category_name";
    private static final String FIELD_PRODUCT_DESC = "product_description";
    private static final String FIELD_RETAIL_PRICE = "retail_price";
    private static final String FIELD_WISH_COUNT = "wish_count";
    private static final String FIELD_IMAGE_URLS = "image_urls";
    private static final String FIELD_USER_ID = "user_id";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_PROVIDER_TYPE = "provider_type";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ORDER_ID = "order_id";
    private static final String FIELD_BUYER_NAME = "buyer_name";
    private static final String FIELD_SELLER_NAME = "seller_name";
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_CURRENT_STATUS = "current_status";
    private static final String FIELD_BID_TYPE = "bid_type";
    private static final String FIELD_INSPECTION_ID = "inspection_id";
    private static final String FIELD_INSPECTOR_NAME = "inspector_name";
    private static final String FIELD_TYPE = "type";

    // ==================== 검색 메서드 ====================

    /**
     * 상품 검색 (회원용 + 관리자용 공통)
     */
    public SearchResultDto<ProductSearchResponseDto> searchProducts(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            increaseSearchCount(keyword.trim());
        }
        try {
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy(FIELD_PRODUCT_NAME + "," + FIELD_BRAND_NAME + "," + FIELD_CATEGORY_NAME)
                    .filterBy(FILTER_IS_DELETED_FALSE)
                    .sortBy(SORT_CREATED_AT_DESC)
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections(COL_PRODUCTS)
                    .documents()
                    .search(params);

            return convertToProductDto(result, pageable);

        } catch (Exception e) {
            log.error("상품 검색 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.SEARCH_ERROR);
        }
    }

    /**
     * 회원 검색 (관리자용)
     */
    public SearchResultDto<UserSearchResponseDto> searchUsers(String keyword, Pageable pageable) {
        try {
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy(FIELD_EMAIL + "," + FIELD_NAME)
                    .filterBy(FILTER_IS_DELETED_FALSE)
                    .sortBy(SORT_CREATED_AT_DESC)
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections(COL_USERS)
                    .documents()
                    .search(params);

            return convertToUserDto(result, pageable);

        } catch (Exception e) {
            log.error("회원 검색 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.SEARCH_ERROR);
        }
    }

    /**
     * 관리자 주문 목록 검색 (MANAGER-009)
     * 키워드 + 상태/날짜 필터 지원
     */
    public Page<AdminOrderResponseDto> searchOrders(
            String keyword, List<String> status,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        try {
            // 검색 조건을 담을 객체 생성
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy(FIELD_PRODUCT_NAME + "," + FIELD_BUYER_NAME + "," + FIELD_SELLER_NAME)
                    .sortBy(SORT_CREATED_AT_DESC)
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            // 필터 조건 동적 조합
            // 날짜 한쪽만 입력된 경우 보정 (OrderService와 일관성)
            if (startDate != null && endDate == null) {
                endDate = LocalDateTime.now();
            }
            if (endDate != null && startDate == null) {
                startDate = LocalDate.parse(serviceStartDate).atStartOfDay();
            }

            List<String> filters = new ArrayList<>();

            // 상태(status) 필터 추가
            if (status != null && !status.isEmpty()) {

                // 리스트 안의 값을 하나씩 검사해서 새로운 리스트로 만들기
                List<String> validatedStatuses = status.stream()
                        .map(s -> {
                            try {
                                return CurrentStatus.valueOf(s).name();
                            } catch (IllegalArgumentException e) {
                                throw new CustomException(ErrorCode.BAD_REQUEST);
                            }
                        })
                        .toList();
                String statusFilter = String.join(",", validatedStatuses);    // 검증된 값들을 콤마로 연결
                filters.add(FIELD_CURRENT_STATUS + ":[" + statusFilter + "]");   // 최종적으로 검색 조건에 추가
            }

            // 시작 날짜 필터 추가
            if (startDate != null) {
                filters.add(FIELD_CREATED_AT + ":>=" + startDate.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
            }

            // 종료 날짜 필터 추가
            if (endDate != null) {
                filters.add(FIELD_CREATED_AT + ":<=" + endDate.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
            }

            // 필터 조건이 있으면 params에 추가
            if (!filters.isEmpty()) {
                params.filterBy(String.join(" && ", filters));
            }

            SearchResult result = typesenseClient.collections(COL_ORDERS)
                    .documents()
                    .search(params);

            return convertToOrderDto(result, pageable);

        } catch (Exception e) {
            log.error("관리자 주문 검색 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.SEARCH_ERROR);
        }
    }

    /**
     * 검수 검색 (관리자용)
     */
    public SearchResultDto<InspectionSearchResponseDto> searchInspections(String keyword, Pageable pageable) {
        try {
            SearchParameters params = new SearchParameters()
                    .q(keyword)
                    .queryBy(FIELD_PRODUCT_NAME + "," + FIELD_SELLER_NAME + "," + FIELD_INSPECTOR_NAME)
                    .sortBy(SORT_CREATED_AT_DESC)
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections(COL_INSPECTIONS)
                    .documents()
                    .search(params);

            return convertToInspectionDto(result, pageable);

        } catch (Exception e) {
            log.error("검수 검색 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.SEARCH_ERROR);
        }
    }

    // ==================== 인덱싱 메서드 ====================

    /**
     * 상품 인덱싱
     */
    public void indexProduct(Product product) {
        try {
            Map<String, Object> document = new HashMap<>();
            document.put(FIELD_ID, String.valueOf(product.getProductId()));
            document.put(FIELD_PRODUCT_ID, product.getProductId());
            document.put(FIELD_PRODUCT_NAME, product.getProductName());
            document.put(FIELD_BRAND_NAME, product.getBrand() != null ? product.getBrand().getBrandName() : "");
            document.put(FIELD_CATEGORY_NAME, product.getCategory() != null ? product.getCategory().getCategoryName() : "");
            document.put(FIELD_PRODUCT_DESC, product.getProductDescription());
            document.put(FIELD_RETAIL_PRICE, product.getRetailPrice());
            document.put(FIELD_WISH_COUNT, product.getWishCount());     /// [추가] 관심 개수 추가
            document.put(FIELD_IS_DELETED, product.isDeleted());
            document.put(FIELD_CREATED_AT, product.getCreatedAt() != null
                    ? product.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);

            /// [추가] 상품 이미지 인덱싱
            List<String> imageUrls = new ArrayList<>();
            if (product.getImages() != null && !product.getImages().isEmpty()){
                imageUrls = product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .toList();
            }
            document.put(FIELD_IMAGE_URLS, imageUrls);

            typesenseClient.collections(COL_PRODUCTS)
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
            document.put(FIELD_ID, String.valueOf(user.getUserId()));
            document.put(FIELD_USER_ID, user.getUserId());
            document.put(FIELD_EMAIL, user.getEmail());
            document.put(FIELD_NAME, user.getName());
            document.put(FIELD_ROLE, user.getRole().name());
            document.put(FIELD_PROVIDER_TYPE, user.getProviderType() != null ? user.getProviderType().name() : "");
            document.put(FIELD_STATUS, user.getStatus().name());
            document.put(FIELD_IS_DELETED, user.getIsDeleted());
            document.put(FIELD_CREATED_AT, user.getCreatedAt() != null
                    ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);   // 검색엔진에서 쓰기 좋은 초 단위 숫자로 바꿔서 저장

            typesenseClient.collections(COL_USERS)
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
            document.put(FIELD_ID, String.valueOf(order.getOrderId()));
            document.put(FIELD_ORDER_ID, order.getOrderId());
            document.put(FIELD_BUYER_NAME, order.getBuyer() != null ? order.getBuyer().getName() : "");
            document.put(FIELD_SELLER_NAME, order.getSeller() != null ? order.getSeller().getName() : "");
            document.put(FIELD_PRODUCT_NAME, order.getProduct() != null ? order.getProduct().getProductName() : "");
            document.put(FIELD_PRICE, order.getPrice());
            document.put(FIELD_CURRENT_STATUS, order.getCurrentStatus() != null ? order.getCurrentStatus().name() : "");
            document.put(FIELD_BID_TYPE, order.getType() != null ? order.getType().name() : "");
            document.put(FIELD_CREATED_AT, order.getCreatedAt() != null
                    ? order.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);

            typesenseClient.collections(COL_ORDERS)
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
            } else if (inspection.getStorageItem() != null) {
                productName = inspection.getStorageItem().getProduct() != null
                        ? inspection.getStorageItem().getProduct().getProductName() : "";
                sellerName = inspection.getStorageItem().getUser() != null
                        ? inspection.getStorageItem().getUser().getName() : "";
            }

            Map<String, Object> document = new HashMap<>();
            document.put(FIELD_ID, String.valueOf(inspection.getInspectionId()));
            document.put(FIELD_INSPECTION_ID, inspection.getInspectionId());
            document.put(FIELD_PRODUCT_NAME, productName);
            document.put(FIELD_SELLER_NAME, sellerName);
            document.put(FIELD_INSPECTOR_NAME, inspection.getUser() != null ? inspection.getUser().getName() : "");
            document.put(FIELD_TYPE, inspection.getType().name());
            document.put(FIELD_STATUS, inspection.getStatus().name());
            document.put(FIELD_CREATED_AT, inspection.getCreatedAt() != null
                    ? inspection.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);

            typesenseClient.collections(COL_INSPECTIONS)
                    .documents()
                    .upsert(document);

            log.info("검수 인덱싱 완료: {}", inspection.getInspectionId());

        } catch (Exception e) {
            log.error("검수 인덱싱 실패: {}", e.getMessage());
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
                try {
                    items.add(mapToProductDto(hit.getDocument()));
                } catch (Exception e) {
                    log.error("상품 DTO 변환 실패: {}", e.getMessage());
                }
            }
        }

        return SearchResultDto.<ProductSearchResponseDto>builder()
                .totalCount(result.getFound() != null ? result.getFound() : 0)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .items(items)
                .build();
    }

    private ProductSearchResponseDto mapToProductDto(Map<String, Object> doc) {
        return ProductSearchResponseDto.builder()
                .productId(doc.get(FIELD_PRODUCT_ID) != null ? ((Number) doc.get(FIELD_PRODUCT_ID)).longValue() : 0L)
                .productName((String) doc.getOrDefault(FIELD_PRODUCT_NAME, ""))
                .brandName((String) doc.getOrDefault(FIELD_BRAND_NAME, ""))
                .categoryName((String) doc.getOrDefault(FIELD_CATEGORY_NAME, ""))
                .productDescription((String) doc.getOrDefault(FIELD_PRODUCT_DESC, ""))
                .wishCount(doc.get(FIELD_WISH_COUNT) != null ? ((Number) doc.get(FIELD_WISH_COUNT)).intValue() : 0)
                .imageUrls((List<String>) doc.get(FIELD_IMAGE_URLS) != null ? (List<String>) doc.get(FIELD_IMAGE_URLS) : null)
                .retailPrice(doc.get(FIELD_RETAIL_PRICE) != null ? ((Number) doc.get(FIELD_RETAIL_PRICE)).intValue() : 0)
                .build();
    }

    private SearchResultDto<UserSearchResponseDto> convertToUserDto(SearchResult result, Pageable pageable) {
        List<UserSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                try {
                    items.add(mapToUserDto(hit.getDocument()));
                } catch (Exception e) {
                    log.error("회원 DTO 변환 실패: {}", e.getMessage());
                }
            }
        }

        return SearchResultDto.<UserSearchResponseDto>builder()
                .totalCount(result.getFound() != null ? result.getFound() : 0)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .items(items)
                .build();
    }

    private UserSearchResponseDto mapToUserDto(Map<String, Object> doc) {
        return UserSearchResponseDto.builder()
                .userId(doc.get(FIELD_USER_ID) != null ? ((Number) doc.get(FIELD_USER_ID)).longValue() : 0L)
                .email((String) doc.getOrDefault(FIELD_EMAIL, ""))
                .name((String) doc.getOrDefault(FIELD_NAME, ""))
                .role((String) doc.getOrDefault(FIELD_ROLE, ""))
                .status((String) doc.getOrDefault(FIELD_STATUS, ""))
                .providerType((String) doc.getOrDefault(FIELD_PROVIDER_TYPE, ""))
                .createdAt(doc.get(FIELD_CREATED_AT) != null
                        ? java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(((Number) doc.get(FIELD_CREATED_AT)).longValue()),
                        java.time.ZoneId.systemDefault())
                        : null)
                .build();
    }

    private Page<AdminOrderResponseDto> convertToOrderDto(SearchResult result, Pageable pageable) {
        List<AdminOrderResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                try {
                    items.add(mapToOrderDto(hit.getDocument()));
                } catch (Exception e) {
                    log.error("관리자 주문 DTO 변환 실패: {}", e.getMessage());
                }
            }
        }

        long totalCount = result.getFound() != null ? result.getFound() : 0;
        return new PageImpl<>(items, pageable, totalCount);
    }

    private AdminOrderResponseDto mapToOrderDto(Map<String, Object> doc) {
        long orderId = doc.get(FIELD_ORDER_ID) != null
                ? ((Number) doc.get(FIELD_ORDER_ID)).longValue() : 0L;

        LocalDateTime createdAt = null;
        if (doc.get(FIELD_CREATED_AT) != null) {
            long epoch = ((Number) doc.get(FIELD_CREATED_AT)).longValue();
            createdAt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(epoch),
                    java.time.ZoneId.systemDefault());
        }

        return AdminOrderResponseDto.builder()
                .orderId(orderId)
                .orderNumber(String.format(OrderService.ORDER_NUMBER_FORMAT, orderId))
                .createdAt(createdAt)
                .buyerName((String) doc.getOrDefault(FIELD_BUYER_NAME, ""))
                .sellerName((String) doc.getOrDefault(FIELD_SELLER_NAME, ""))
                .productName((String) doc.getOrDefault(FIELD_PRODUCT_NAME, ""))
                .price(doc.get(FIELD_PRICE) != null ? ((Number) doc.get(FIELD_PRICE)).intValue() : 0)
                .currentStatus((String) doc.getOrDefault(FIELD_CURRENT_STATUS, ""))
                .build();
    }

    private SearchResultDto<InspectionSearchResponseDto> convertToInspectionDto(SearchResult result, Pageable pageable) {
        List<InspectionSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                try {
                    items.add(mapToInspectionDto(hit.getDocument()));
                } catch (Exception e) {
                    log.error("검수 DTO 변환 실패: {}", e.getMessage());
                }
            }
        }

        return SearchResultDto.<InspectionSearchResponseDto>builder()
                .totalCount(result.getFound() != null ? result.getFound() : 0)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .items(items)
                .build();
    }

    private InspectionSearchResponseDto mapToInspectionDto(Map<String, Object> doc) {
        LocalDateTime createdAt = null;
        if (doc.get(FIELD_CREATED_AT) != null) {
            long epoch = ((Number) doc.get(FIELD_CREATED_AT)).longValue();
            createdAt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(epoch),
                    java.time.ZoneId.systemDefault());
        }

        return InspectionSearchResponseDto.builder()
                .inspectionId(doc.get(FIELD_INSPECTION_ID) != null ? ((Number) doc.get(FIELD_INSPECTION_ID)).longValue() : 0L)
                .productName((String) doc.getOrDefault(FIELD_PRODUCT_NAME, ""))
                .sellerName((String) doc.getOrDefault(FIELD_SELLER_NAME, ""))
                .inspectorName((String) doc.getOrDefault(FIELD_INSPECTOR_NAME, ""))
                .type((String) doc.getOrDefault(FIELD_TYPE, ""))
                .status((String) doc.getOrDefault(FIELD_STATUS, ""))
                .createdAt(createdAt)
                .build();
    }

    // ==================== 유틸 메서드 ====================



    @PostConstruct
    public void init() {
        // 백그라운드에서 재시도하며 초기화 (커스텀 Executor 사용)
        CompletableFuture.runAsync(() -> {
            int maxRetries = 10;
            int retryDelay = 2000;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    log.info(" Typesense 연결 시도 {}/{}", attempt, maxRetries);

                    // 연결 테스트
                    typesenseClient.health.retrieve();
                    log.info(" Typesense 연결 성공!");

                    // 컬렉션 초기화
                    String[] collections = {"products", "users", "orders", "inspections"};
                    for (String name : collections) {
                        if (!collectionExists(name)) {
                            log.warn("컬렉션 없음: {} - 자동 생성", name);
                            createCollection(name);
                        } else {
                            log.info("컬렉션 확인: {}", name);
                        }
                    }

                    log.info("Typesense 초기화 완료!");
                    return;

                } catch (Exception e) {
                    log.warn("시도 {}/{} 실패: {}", attempt, maxRetries, e.getMessage());

                    if (attempt == maxRetries) {
                        log.error(" Typesense 최종 실패 - 검색 기능 제한됨");
                        return;
                    }

                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, taskExecutor); // taskExecutor 전달

        log.info(" Spring 시작 완료 (Typesense는 백그라운드 초기화 중)");
    }

    public boolean collectionExists(String collectionName) {
        try {
            typesenseClient.collections(collectionName).retrieve();
            return true;
        } catch (Exception e) {
            if (e.getMessage().contains("404") ||
                    e.getClass().getSimpleName().equals("ObjectNotFound")) {
                return false;
            }

            // 연결 오류는 경고만
            log.warn(" Typesense 체크 실패: {}", e.getMessage());
            return false;  // 예외 던지지 않음!
        }
    }

    /**
     * 컬렉션 자동 생성
     */
    private void createCollection(String collectionName) {
        try {
            List<Field> fields = switch (collectionName) {
                case "products" -> List.of(
                        new Field().name("product_id").type("int64"),
                        new Field().name("product_name").type("string"),
                        new Field().name("brand_name").type("string"),
                        new Field().name("category_name").type("string"),
                        new Field().name("product_description").type("string"),
                        new Field().name("retail_price").type("int32"),
                        new Field().name("is_deleted").type("bool"),
                        new Field().name("image_urls").type("string[]"),
                        new Field().name("wish_count").type("int64"),
                        new Field().name("created_at").type("int64")
                );
                case "users" -> List.of(
                        new Field().name("user_id").type("int64"),
                        new Field().name("email").type("string"),
                        new Field().name("name").type("string"),
                        new Field().name("role").type("string"),
                        new Field().name("status").type("string"),
                        new Field().name("provider_type").type("string"),
                        new Field().name("is_deleted").type("bool"),
                        new Field().name("created_at").type("int64")
                );
                case "orders" -> List.of(
                        new Field().name("order_id").type("int64"),
                        new Field().name("buyer_name").type("string"),
                        new Field().name("seller_name").type("string"),
                        new Field().name("product_name").type("string"),
                        new Field().name("price").type("int32"),
                        new Field().name("current_status").type("string"),
                        new Field().name("bid_type").type("string"),
                        new Field().name("created_at").type("int64")
                );
                case "inspections" -> List.of(
                        new Field().name("inspection_id").type("int64"),
                        new Field().name("product_name").type("string"),
                        new Field().name("seller_name").type("string"),
                        new Field().name("inspector_name").type("string"),
                        new Field().name("type").type("string"),
                        new Field().name("status").type("string"),
                        new Field().name("created_at").type("int64")
                );
                default -> throw new IllegalArgumentException("Unknown collection: " + collectionName);
            };

            CollectionSchema schema = new CollectionSchema();
            schema.name(collectionName);
            schema.fields(fields);
            schema.defaultSortingField("created_at");

            typesenseClient.collections().create(schema);
            log.info("Typesense 컬렉션 자동 생성 완료: {}", collectionName);

        } catch (Exception e) {
            /// 에러 메시지나 코드를 확인하여 "이미 존재함" 에러인지 판단
            if (e.getMessage().contains("already exists") || e.getMessage().contains("409")) {
                log.info("Typesense 컬렉션이 이미 존재함 (생성 건너뜀): {}", collectionName);
            } else {
                // 그 외의 진짜 에러만 로그에 남김
                log.error("Typesense 컬렉션 생성 실패: {} - {}", collectionName, e.getMessage());
            }
        }
    }
    /**
     * 검색어 횟수 증가 (Redis ZSET)
     */
    private void increaseSearchCount(String keyword) {
        try {
            Double updatedScore = redisTemplate.opsForZSet().incrementScore(POPULAR_KEYWORD_KEY, keyword, 1.0);
            log.info("인기 검색어 카운트 성공: {}", updatedScore);
        } catch (Exception e) {
            log.error("인기 검색어 카운트 실패: {}",e.getMessage());
        }
    }

    /**
     * 인기 검색어 점수 감소 및 정리 (매일 자정 실행)
     * - 모든 검색어 점수에 0.9 곱하기 (10% 감소)
     * - 점수가 1.0 미만인 검색어 삭제
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void decayPopularKeywords() {
        try {
            /// 1. 모든 키워드 점수에 0.9 곱하기 (ZUNIONSTORE 사용)
            /// ZUNIONSTORE search:popular 1 search:popular WEIGHTS 0.9
            redisTemplate.opsForZSet().unionAndStore(POPULAR_KEYWORD_KEY, List.of(POPULAR_KEYWORD_KEY), List.of(0.9).toString(), Aggregate.valueOf(POPULAR_KEYWORD_KEY));

            /// 2. 점수가 1.0 미만인 키워드 삭제
            /// ZREMRANGEBYSCORE search:popular -inf (1
            /// 1.0 미만을 표현하기 위해 0.999... 또는 rangeByScoreLimiting 사용 가능하지만,
            /// 여기서는 1.0 미만 삭제를 위해 0부터 0.9999까지 삭제 등으로 처리하거나,
            /// 정확히는 removeRangeByScore(key, 0, 0.999999)
            Long removedCount = redisTemplate.opsForZSet().removeRangeByScore(POPULAR_KEYWORD_KEY, 0, 0.99999);

            log.info("인기 검색어 점수 감소 완료. 삭제된 검색어 수: {}", removedCount);

        } catch (Exception e) {
            log.error("인기 검색어 점수 감소 스케줄러 실패: {}", e.getMessage());
        }
    }

    /**
     * 인기 검색어 Top 10 조회
     */
    public List<String> getPopularKeywords() {
        try {
            /// Score(검색 횟수)가 높은 순으로 상위 10개 조회 (Reverse Range)
            Set<String> topKeywords = redisTemplate.opsForZSet().reverseRange(POPULAR_KEYWORD_KEY, 0, 9);
            log.info("인기 검색어 조회 : {}",topKeywords);
            if (topKeywords == null || topKeywords.isEmpty()) {
                return List.of();
            }
            return new ArrayList<>(topKeywords);


        } catch (Exception e) {
            log.error("인기 검색어 조회 실패 : {}",e.getMessage());
            return List.of();
        }
    }
}