package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.WishResponseDto;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.entity.WishList;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.ProductRepository;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.repository.WishListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishListRepository wishListRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

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

        // 5. wish_count +1 (Atomic UPDATE)
        productRepository.incrementWishCount(productId);

        // 6. 응답 반환
        return WishResponseDto.builder()
                .wishCount(product.getWishCount() + 1)
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

        // 3. wish_count -1 (Atomic UPDATE)
        productRepository.decrementWishCount(productId);

        // 4. 응답 반환
        Product product = wishList.getProduct();
        return WishResponseDto.builder()
                .wishCount(Math.max(product.getWishCount() - 1, 0))
                .build();
    }
}