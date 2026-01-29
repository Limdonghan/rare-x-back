package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.BidInfo;
import com.project.rare_x_back.dto.response.ProductDetailResponseDto;
import com.project.rare_x_back.dto.response.ProductResponseDto;
import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.ProductImage;
import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.BuyBidRepository;
import com.project.rare_x_back.repository.ProductRepository;
import com.project.rare_x_back.repository.SaleBidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BuyBidRepository buyBidRepository;
    private final SaleBidRepository saleBidRepository;

    /**
    * [상품 목록 조회]
    * */
    public Page<ProductResponseDto> getAllPublicProd(Long categoryId, Long brandId, Pageable pageable) {

        Page<Product> productPage;

        // 카테고리 + 브랜드 필터 적용 하는 경우
        if (categoryId != null && brandId != null) {
            productPage = productRepository.findByCategory_CategoryIdAndBrand_BrandIdAndIsDeletedFalse(categoryId, brandId, pageable);
        } else if (categoryId != null) { // 카테고리만 필터링
            productPage = productRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId, pageable);
        } else if (brandId != null) { // 브랜드만 필터링
            productPage = productRepository.findByBrand_BrandIdAndIsDeletedFalse(brandId, pageable);
        } else {
                productPage = productRepository.findAllByIsDeletedFalse(pageable);
        }
        // DTO 변환 및 이미지 처리
        return productPage.map(product -> {
            // [추가] 구매 가격 리스트 조회
            List<BuyBid> buyBidPriceList = buyBidRepository.findByProductAndStatusOrderByPriceAsc(product, BidStatus.OPEN);
            List<SaleBid> saleBidPriceList = saleBidRepository.findByProductAndStatusOrderByPriceAsc(product, BidStatus.OPEN);

            // [추가] 즉시 구매/판매가 결정 (리스트가 비어있으면 0원)
            int buyPrice = buyBidPriceList.isEmpty() ? 0 : buyBidPriceList.getFirst().getPrice();


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
                    .build();
        });
    }

    /**
    * [상품 목록 상세 조회]
    * */
    public ProductDetailResponseDto getPublicDetailProduct(Long productId) {
        Product product = productRepository.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "상품을 찾을 수 없습니다."
                ));

        // [추가] 'OPEN' 상태인 모든 입찰 내역 조회
        List<BuyBid> allBuyBids = buyBidRepository.findAllByProduct_ProductIdAndStatus(product.getProductId(), BidStatus.OPEN);
        List<SaleBid> allSaleBids = saleBidRepository.findAllByProduct_ProductIdAndStatus(product.getProductId(), BidStatus.OPEN);

        // Java Stream으로 그룹핑 & 카운트 & 정렬
        // 구매 입찰 리스트: 가격별로 묶기 -> 내림차순
        List<BidInfo> buyBidList = allBuyBids.stream()
                .collect(Collectors.groupingBy(BuyBid::getPrice, Collectors.counting()))
                .entrySet().stream()
                .map(integerLongEntry -> new BidInfo(integerLongEntry.getKey(), integerLongEntry.getValue()))
                .sorted(Comparator.comparingInt(BidInfo::getPrice).reversed())
                .toList();

        // 판매 입찰 리스트: 가격별로 묶기 -> 오름차순
        List<BidInfo> saleBidList = allSaleBids.stream()
                .collect(Collectors.groupingBy(SaleBid::getPrice, Collectors.counting()))
                .entrySet().stream()
                .map(integerLongEntry -> new BidInfo(integerLongEntry.getKey(), integerLongEntry.getValue()))
                .sorted(Comparator.comparingInt(BidInfo::getPrice))
                .toList();


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
                .build();

    }

}
