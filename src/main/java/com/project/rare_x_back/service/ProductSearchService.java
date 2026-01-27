package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.ProductSearchResponseDto;
import com.project.rare_x_back.dto.response.ProductSearchResultDto;
import com.project.rare_x_back.entity.Product;
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
public class ProductSearchService {

    private final Client typesenseClient;

    private static final String COLLECTION_NAME = "products";

    /**
     * 상품 검색
     */
    public ProductSearchResultDto search(String keyword, Pageable pageable) {
        try {
            SearchParameters searchParameters = new SearchParameters()
                    .q(keyword)
                    .queryBy("product_name,brand_name,category_name")
                    .page(pageable.getPageNumber() + 1)
                    .perPage(pageable.getPageSize());

            SearchResult result = typesenseClient.collections(COLLECTION_NAME)
                    .documents()
                    .search(searchParameters);

            return convertToDto(result, pageable);

        } catch (Exception e) {
            log.error("Typesense 검색 실패: {}", e.getMessage());
            throw new RuntimeException("검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * SearchResult -> DTO 변환
     */
    private ProductSearchResultDto convertToDto(SearchResult result, Pageable pageable) {
        List<ProductSearchResponseDto> products = new ArrayList<>();

        if (result.getHits() != null) {
            for (SearchResultHit hit : result.getHits()) {
                Map<String, Object> doc = hit.getDocument();

                ProductSearchResponseDto dto = ProductSearchResponseDto.builder()
                        .productId(((Number) doc.get("product_id")).longValue())
                        .productName((String) doc.get("product_name"))
                        .brandName((String) doc.get("brand_name"))
                        .categoryName((String) doc.get("category_name"))
                        .productDescription((String) doc.get("product_description"))
                        .retailPrice(((Number) doc.get("retail_price")).intValue())
                        .build();

                products.add(dto);
            }
        }

        return ProductSearchResultDto.builder()
                .totalCount(result.getFound())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .products(products)
                .build();
    }

    /**
     * 상품 인덱싱 (등록/수정 시 호출)
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

            typesenseClient.collections(COLLECTION_NAME)
                    .documents()
                    .upsert(document);

            log.info("상품 인덱싱 완료: {}", product.getProductId());

        } catch (Exception e) {
            log.error("상품 인덱싱 실패: {}", e.getMessage());
        }
    }

    /**
     * 상품 인덱스 삭제 (삭제 시 호출)
     */
    public void deleteProduct(Long productId) {
        try {
            typesenseClient.collections(COLLECTION_NAME)
                    .documents(String.valueOf(productId))
                    .delete();

            log.info("상품 인덱스 삭제 완료: {}", productId);

        } catch (Exception e) {
            log.error("상품 인덱스 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * 전체 상품 동기화 (초기 인덱싱)
     */
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
}