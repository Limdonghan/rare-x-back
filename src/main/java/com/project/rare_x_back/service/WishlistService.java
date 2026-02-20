package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.WishResponseDto;
import com.project.rare_x_back.dto.response.WishlistResponseDto;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.entity.WishList;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishListRepository wishListRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SaleBidRepository saleBidRepository;
    private final BuyBidRepository buyBidRepository;

    // ===== WISH-002 관심 상품 등록 =====
    @Transactional
    public WishResponseDto addWish(Long userId, Long productId) {
        // 1. 상품 조회 + 삭제 여부 체크
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new CustomException(ErrorCode.PRODUCT_DELETED);
        }

        // 2. 이미 찜한 상품인지 확인
        if (wishListRepository.existsByUserUserIdAndProductProductId(userId, productId)) {
            throw new CustomException(ErrorCode.WISH_ALREADY_EXISTS);
        }

        // 3. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. wish_lists INSERT
        WishList wishList = WishList.builder()
                .product(product)
                .user(user)
                .build();
        wishListRepository.save(wishList);
        wishListRepository.flush();

        // 5. wish_count +1 (Atomic UPDATE)
        productRepository.incrementWishCount(productId);

        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        return WishResponseDto.builder()
                .wishCount(updatedProduct.getWishCount())
                .build();
    }

    // ===== WISH-002 관심 상품 해제 =====
    @Transactional
    public WishResponseDto removeWish(Long userId, Long productId) {
        // 1. 찜 여부 확인
        WishList wishList = wishListRepository.findByUserUserIdAndProductProductId(userId, productId)
                .orElseThrow(() -> new CustomException(ErrorCode.WISH_NOT_FOUND));

        // 2. wish_lists DELETE
        wishListRepository.delete(wishList);
        wishListRepository.flush();

        // 3. wish_count -1 (Atomic UPDATE)
        productRepository.decrementWishCount(productId);

        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        return WishResponseDto.builder()
                .wishCount(updatedProduct.getWishCount())
                .build();
    }

    // ===== WISH-001 관심 상품 목록 조회 =====
    @Transactional(readOnly = true)
    public Page<WishlistResponseDto> getMyWishlist(Long userId, Pageable pageable) {
        // 1단계: 위시리스트 + 상품 정보 페이징 조회
        Page<WishList> wishPage = wishListRepository.findByUserUserIdAndProductIsDeletedFalse(userId, pageable);

        if (wishPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2단계: 조회된 상품 ID 목록으로 최저가 배치 조회(N+1 문제 방지)
        List<Long> productIds = wishPage.getContent().stream()
                .map(w -> w.getProduct().getProductId())
                .collect(Collectors.toList());

        Map<Long, Integer> lowestPriceMap = saleBidRepository
                .findLowestPriceByProductIds(productIds, BidStatus.OPEN)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],   // 상품ID
                        row -> (Integer) row[1] // 최저가 BUY NOW
                ));

        Map<Long, Integer> HighestPrice = buyBidRepository
                .findHighestPriceByProductIds(productIds, BidStatus.OPEN)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],   // 상품ID
                        row -> (Integer) row[1] // 최고가 SELL NOW
                ));



        // 3단계: DTO 변환
        return wishPage.map(wish -> {
            Product product = wish.getProduct();
            String imageUrl = product.getImages().isEmpty()
                    ? null
                    : product.getImages().get(0).getImageUrl();
            String brandName = product.getBrand() != null
                    ? product.getBrand().getBrandName()
                    : "";

            return WishlistResponseDto.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .brandName(brandName)
                    .productImageUrl(imageUrl)
                    .lowestPrice(lowestPriceMap.get(product.getProductId()))
                    .HighestPrice(HighestPrice.get(product.getProductId()))
                    .wishCount(product.getWishCount())
                    .createdAt(wish.getCreatedAt())
                    .build();
        });
    }
}