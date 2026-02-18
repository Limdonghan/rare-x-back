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
                    .queryBy("product_name,brand_name,category_name")
                    .filterBy("is_deleted:false")
                    .sortBy("created_at:desc")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections("products")
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
                    .queryBy("email,name")
                    .filterBy("is_deleted:false")
                    .sortBy("created_at:desc")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections("users")
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
                    .queryBy("product_name,buyer_name,seller_name")
                    .sortBy("created_at:desc")
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
                filters.add("current_status:[" + statusFilter + "]");   // 최종적으로 검색 조건에 추가
            }

            // 시작 날짜 필터 추가
            if (startDate != null) {
                filters.add("created_at:>=" + startDate.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
            }

            // 종료 날짜 필터 추가
            if (endDate != null) {
                filters.add("created_at:<=" + endDate.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
            }

            // 필터 조건이 있으면 params에 추가
            if (!filters.isEmpty()) {
                params.filterBy(String.join(" && ", filters));
            }

            SearchResult result = typesenseClient.collections("orders")
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
                    .queryBy("product_name,seller_name,inspector_name")
                    .sortBy("created_at:desc")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections("inspections")
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
            document.put("id", String.valueOf(product.getProductId()));
            document.put("product_id", product.getProductId());
            document.put("product_name", product.getProductName());
            document.put("brand_name", product.getBrand() != null ? product.getBrand().getBrandName() : "");
            document.put("category_name", product.getCategory() != null ? product.getCategory().getCategoryName() : "");
            document.put("product_description", product.getProductDescription());
            document.put("retail_price", product.getRetailPrice());
            document.put("wish_count", product.getWishCount());     /// [추가] 관심 개수 추가
            document.put("is_deleted", product.isDeleted());
            document.put("created_at", product.getCreatedAt() != null
                    ? product.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);

            /// [추가] 상품 이미지 인덱싱
            List<String> imageUrls = new ArrayList<>();
            if (product.getImages() != null && !product.getImages().isEmpty()){
                imageUrls = product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .toList();
            }
            document.put("image_urls", imageUrls);

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
            document.put("provider_type", user.getProviderType() != null ? user.getProviderType().name() : "");
            document.put("status", user.getStatus().name());
            document.put("is_deleted", user.getIsDeleted());
            document.put("created_at", user.getCreatedAt() != null
                    ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);   // 검색엔진에서 쓰기 좋은 초 단위 숫자로 바꿔서 저장

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
            document.put("bid_type", order.getType() != null ? order.getType().name() : "");
            document.put("created_at", order.getCreatedAt() != null
                    ? order.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);

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
            document.put("created_at", inspection.getCreatedAt() != null
                    ? inspection.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);

            typesenseClient.collections("inspections")
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
                    Map<String, Object> doc = hit.getDocument();
                    items.add(ProductSearchResponseDto.builder()
                            .productId(doc.get("product_id") != null ? ((Number) doc.get("product_id")).longValue() : 0L)
                            .productName((String) doc.getOrDefault("product_name", ""))
                            .brandName((String) doc.getOrDefault("brand_name", ""))
                            .categoryName((String) doc.getOrDefault("category_name", ""))
                            .productDescription((String) doc.getOrDefault("product_description", ""))
                            .wishCount((Integer) doc.getOrDefault("wish_count", 0))
                            .imageUrls((List<String>) doc.get("image_urls") != null ? (List<String>) doc.get("image_urls") : null)
                            .retailPrice(doc.get("retail_price") != null ? ((Number) doc.get("retail_price")).intValue() : 0)
                            .build());
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

    private SearchResultDto<UserSearchResponseDto> convertToUserDto(SearchResult result, Pageable pageable) {
        List<UserSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                try {
                    Map<String, Object> doc = hit.getDocument();
                    items.add(UserSearchResponseDto.builder()
                            .userId(doc.get("user_id") != null ? ((Number) doc.get("user_id")).longValue() : 0L)
                            .email((String) doc.getOrDefault("email", ""))
                            .name((String) doc.getOrDefault("name", ""))
                            .role((String) doc.getOrDefault("role", ""))
                            .status((String) doc.getOrDefault("status", ""))
                            .providerType((String) doc.getOrDefault("provider_type", ""))
                            .createdAt(doc.get("created_at") != null
                                    ? java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochSecond(((Number) doc.get("created_at")).longValue()),
                                    java.time.ZoneId.systemDefault())
                                    : null)
                            .build());
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

    private Page<AdminOrderResponseDto> convertToOrderDto(SearchResult result, Pageable pageable) {
        List<AdminOrderResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                try {
                    Map<String, Object> doc = hit.getDocument();
                    long orderId = doc.get("order_id") != null
                            ? ((Number) doc.get("order_id")).longValue() : 0L;

                    LocalDateTime createdAt = null;
                    if (doc.get("created_at") != null) {
                        long epoch = ((Number) doc.get("created_at")).longValue();
                        createdAt = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochSecond(epoch),
                                java.time.ZoneId.systemDefault());
                    }

                    items.add(AdminOrderResponseDto.builder()
                            .orderId(orderId)
                            .orderNumber(String.format("ORD-%08d", orderId))
                            .createdAt(createdAt)
                            .buyerName((String) doc.getOrDefault("buyer_name", ""))
                            .sellerName((String) doc.getOrDefault("seller_name", ""))
                            .productName((String) doc.getOrDefault("product_name", ""))
                            .price(doc.get("price") != null ? ((Number) doc.get("price")).intValue() : 0)
                            .currentStatus((String) doc.getOrDefault("current_status", ""))
                            .build());
                } catch (Exception e) {
                    log.error("관리자 주문 DTO 변환 실패: {}", e.getMessage());
                }
            }
        }

        long totalCount = result.getFound() != null ? result.getFound() : 0;
        return new PageImpl<>(items, pageable, totalCount);
    }

    private SearchResultDto<InspectionSearchResponseDto> convertToInspectionDto(SearchResult result, Pageable pageable) {
        List<InspectionSearchResponseDto> items = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                try {
                    Map<String, Object> doc = hit.getDocument();
                    items.add(InspectionSearchResponseDto.builder()
                            .inspectionId(doc.get("inspection_id") != null ? ((Number) doc.get("inspection_id")).longValue() : 0L)
                            .productName((String) doc.getOrDefault("product_name", ""))
                            .sellerName((String) doc.getOrDefault("seller_name", ""))
                            .inspectorName((String) doc.getOrDefault("inspector_name", ""))
                            .type((String) doc.getOrDefault("type", ""))
                            .status((String) doc.getOrDefault("status", ""))
                            .build());
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