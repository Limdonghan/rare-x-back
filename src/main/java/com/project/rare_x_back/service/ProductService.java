package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.BidInfo;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.ProductImage;
import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BuyBidRepository buyBidRepository;
    private final SaleBidRepository saleBidRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final WishListRepository wishListRepository;
    private final OrderRepository orderRepository;

    /**
    * [상품 목록 조회]
    * */
    public Page<ProductResponseDto> getAllPublicProd(List<Long> categoryIds, List<Long> brandIds, Pageable pageable) {

        Page<Product> productPage;

        boolean hasCategory = categoryIds != null && !categoryIds.isEmpty();    // null 체크 + 빈 리스트 체크
        boolean hasBrand = brandIds != null && !brandIds.isEmpty();    // null 체크 + 빈 리스트 체크

        // 카테고리 + 브랜드 필터 적용 하는 경우
        if (hasCategory && hasBrand) {
            productPage = productRepository.findByCategory_CategoryIdInAndBrand_BrandIdInAndIsDeletedFalse(categoryIds, brandIds, pageable);
        } else if (hasCategory) {
            productPage = productRepository.findByCategory_CategoryIdInAndIsDeletedFalse(categoryIds, pageable);
        } else if (hasBrand) {
            productPage = productRepository.findByBrand_BrandIdInAndIsDeletedFalse(brandIds, pageable);
        } else {
            productPage = productRepository.findAllByIsDeletedFalse(pageable);
        }
        // DTO 변환 및 이미지 처리
        return productPage.map(product -> {
            // [추가] 구매 가격 리스트 조회
            List<SaleBid> saleBidPriceList = saleBidRepository.findByProductAndStatusOrderByPriceAsc(product, BidStatus.OPEN);

            // [추가] 즉시 구매/판매가 결정 (리스트가 비어있으면 0원)
            int buyPrice = saleBidPriceList.isEmpty() ? 0 : saleBidPriceList.getFirst().getPrice();


            // 이미지 리스트에서 첫 번째 이미지(썸네일) URL 추출
            String imageUrl = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().getFirst().getImageUrl();
            }

            return ProductResponseDto.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : "")
                    .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : "")
                    .price(buyPrice)
                    .imageUrl(imageUrl) // 추출한 S3 URL 주입 (썸네일)
                    .wishCount(product.getWishCount())
                    .build();
        });
    }

    /**
    * [상품 목록 상세 조회]
    * */
    public ProductDetailResponseDto getPublicDetailProduct(Long productId, Long userId) {
        Product product = productRepository.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "상품을 찾을 수 없습니다."
                ));

        // [추가] 'OPEN' 상태인 모든 입찰 내역 조회
        List<BuyBid> allBuyBids = buyBidRepository.findAllByProduct_ProductIdAndStatus(product.getProductId(), BidStatus.OPEN);
        List<SaleBid> allSaleBids = saleBidRepository.findAllByProduct_ProductIdAndStatus(product.getProductId(), BidStatus.OPEN);

        // 내 입찰 가격 추출
        List<Integer> myBuyPrices =
                (userId == null) ? List.of() :
                        allBuyBids.stream()
                                .filter(b -> b.getUser().getUserId().equals(userId))
                                .map(BuyBid::getPrice)
                                .distinct()
                                .toList();

        List<Integer> mySellPrices =
                (userId == null) ? List.of() :
                        allSaleBids.stream()
                                .filter(s -> s.getUser().getUserId().equals(userId))
                                .map(SaleBid::getPrice)
                                .distinct()
                                .toList();

        // [최적화] 1. 원본 리스트 미리 정렬 (O(N log N))
        // 구매 입찰: 가격 내림차순 -> 시간 오름차순 (같은 가격이면 먼저 등록된게 우선)
        allBuyBids.sort(Comparator.comparingInt(BuyBid::getPrice).reversed()
                .thenComparing(BuyBid::getCreatedAt));

        // 판매 입찰: 가격 오름차순 -> 시간 오름차순 (같은 가격이면 먼저 등록된게 우선)
        allSaleBids.sort(
                Comparator
                        .comparingInt(SaleBid::getPrice) // 1순위 가격 낮은 순
                        .thenComparing(
                                (SaleBid s) -> s.getStorageItem() != null, // 동일 조건이면 보관 우선
                                Comparator.reverseOrder()
                        )
                        .thenComparing(SaleBid::getCreatedAt) // 보관 없으면 시간 우선
        );

        // [최적화] 2. 정렬된 리스트에서 즉시 ID 추출 (O(1))
        Long highestBuyBidId = allBuyBids.isEmpty() ? null : allBuyBids.get(0).getBuyId();
        Long lowestSaleBidId = allSaleBids.isEmpty() ? null : allSaleBids.get(0).getSellId();

        // [최적화] 3. 정렬된 순서 유지하며 그룹핑 (O(N)) - LinkedHashMap 사용
        // 구매 입찰 리스트: 가격별로 묶기 (이미 정렬되어 있음)
        List<BidInfo> buyBidList = allBuyBids.stream()
                .collect(Collectors.groupingBy(
                        BuyBid::getPrice,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {

                    int price = entry.getKey();
                    List<BuyBid> bids = entry.getValue();

                    long quantity = bids.size();

                    boolean isMine = userId != null &&
                            bids.stream().anyMatch(b -> b.getUser().getUserId().equals(userId));

                    return new BidInfo(price, quantity, isMine);
                })
                .toList();

        // 판매 입찰 리스트: 가격별로 묶기 (이미 정렬되어 있음)
        List<BidInfo> saleBidList = allSaleBids.stream()
                .collect(Collectors.groupingBy(
                        SaleBid::getPrice,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {

                    int price = entry.getKey();
                    List<SaleBid> bids = entry.getValue();

                    long quantity = bids.size();
                    // 내 입찰 여부
                    boolean isMine = userId != null &&
                            bids.stream().anyMatch(b -> b.getUser().getUserId().equals(userId));

                    long storageQuantity =
                            bids.stream()
                                    .filter(b -> b.getStorageItem() != null)
                                    .count();

                    boolean isStorageSale = storageQuantity > 0;

//                    // 보관 판매 포함 여부
//                    boolean isStorageSale =
//                            bids.stream().anyMatch(b -> b.getStorageItem() != null);


                    return new BidInfo(price, quantity, isMine,  isStorageSale, storageQuantity);
                })
                .toList();

        boolean hasMyBuyBid = userId != null &&
                allBuyBids.stream()
                        .anyMatch(b -> b.getUser().getUserId().equals(userId));

        boolean hasMySellBid = userId != null &&
                allSaleBids.stream()
                        .anyMatch(b -> b.getUser().getUserId().equals(userId));


        // [추가] 즉시 구매/판매가 결정 (리스트가 비어있으면 0원)
        int buyPrice = saleBidList.isEmpty() ? 0 : saleBidList.getFirst().getPrice();

        // [추가] 상품 즉시 판매 최저가, 입찰이 없으며 0원
        int salePrice = buyBidList.isEmpty() ? 0 : buyBidList.getFirst().getPrice();


        // 이미지 객체 리스트를 URL만 있는 문자열 리스트로 변환
        // 리스트가 null이면 빈 리스트 반환, 있으면 스트림
        List<String> imageUrls = product.getImages() == null
                ? List.of()
                :product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        // 찜 여부 확인 (비로그인이면 false)
        boolean isLiked = false;
        if (userId != null) {
            isLiked = wishListRepository.existsByUserUserIdAndProductProductId(userId, productId);
        }

        return ProductDetailResponseDto.builder()
                .productId(productId)
                .productName(product.getProductName())
                .productDescription(product.getProductDescription())
                .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : "")
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : "")
                .imageUrls(imageUrls)
                .buyPrice(buyPrice)
                .salePrice(salePrice)
                .buyBidInfoList(buyBidList)
                .saleBidInfoList(saleBidList)
                .retailPrice(product.getRetailPrice())
                .wishCount(product.getWishCount())
                .isLiked(isLiked)
                .highestBuyBidId(highestBuyBidId)
                .lowestSaleBidId(lowestSaleBidId)
                .hasMyBuyBid(hasMyBuyBid)
                .hasMySellBid(hasMySellBid)
                .buyBidInfoList(buyBidList)
                .saleBidInfoList(saleBidList)
                .build();

    }

    /**
     * [공용 카테고리 목록 조회]
     */
    public List<CategoryListResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> CategoryListResponseDto.builder()
                        .categoryId(category.getCategoryId())
                        .categoryName(category.getCategoryName())
                        .productCount(productRepository.countByCategory_CategoryIdAndIsDeletedFalse(category.getCategoryId()))
                        .createdAt(category.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * [공용 브랜드 목록 조회]
     */
    public List<BrandListResponseDto> getAllBrands() {
        return brandRepository.findBrandsByIsDeletedFalse(Pageable.unpaged()).stream()
                .map(brand -> BrandListResponseDto.builder()
                        .brandId(brand.getBrandId())
                        .brandName(brand.getBrandName())
                        .productCount(productRepository.countByBrand_BrandIdAndIsDeletedFalse(brand.getBrandId()))
                        .createdAt(brand.getCreatedAt())
                        .build())
                .toList();
    }
    /**
     * [보관 판매 상품 목록 조회]
     */
    @Transactional(readOnly = true)
    public List<StorageProductResponseDto> getStorageProducts() {
        List<StorageProductResponseDto> results = saleBidRepository.findStorageProducts(BidStatus.OPEN);

        /// 상품 ID 목록 추출
        List<Long> productIds = results.stream()
                .map(StorageProductResponseDto::getProductId)
                .toList();

        /// 상품 정보 일괄 조회 (이미지 정보를 가져오기 위함)
        /// findAllById 대신 EntityGraph가 적용된 메서드 사용으로 N+1 문제 해결
        Map<Long, Product> productMap = productRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, product -> product));

        /// 이미지(썸네일) URL 세팅
        results.forEach(dto -> {
            Product product = productMap.get(dto.getProductId());
            if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
                dto.setImageUrl(product.getImages().getFirst().getImageUrl());
            }
        });

        return results;
    }

    // 랭킹
    public List<ProductRankingProjection> getRanking(String period) {

        int limit = 30; // 랭킹 개수

        LocalDateTime startDate = getStartDate(period);

        return orderRepository.findRanking(startDate, limit);
    }

    // 기간 분기 메서드
    private LocalDateTime getStartDate(String period) {

        return switch (period) {

            case "7d" -> LocalDateTime.now().minusDays(7);

            case "30d" -> LocalDateTime.now().minusDays(30);

            case "90d" -> LocalDateTime.now().minusDays(90);

            case "all" -> LocalDateTime.of(2026, 1, 1, 0, 0);

            default -> LocalDateTime.now().minusDays(7);
        };
    }

}
