package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.*;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import com.project.rare_x_back.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BuyBidRepository buyBidRepository;
    private final SaleBidRepository saleBidRepository;
    private final SettlementRepository settlementRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final StorageItemRepository storageItemRepository;
    private final StorageRequestRepository storageRequestRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final SearchService searchService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    // 진행 중인 주문 상태
    private static final List<CurrentStatus> ACTIVE_ORDER_STATUSES = List.of(
            CurrentStatus.PENDING,
            CurrentStatus.SHIPPED_TO_WAREHOUSE,
            CurrentStatus.PENDING_INSPECTION,
            CurrentStatus.INSPECTING,
            CurrentStatus.PASSED,
            CurrentStatus.SHIPPED
    );

    // 보관 중 상태
    private static final List<StorageStatus> ACTIVE_STORAGE_STATUSES = List.of(
            StorageStatus.STORED,
            StorageStatus.ON_SALE,
            StorageStatus.SUSPENDED
    );

    // 보관 신청 진행 중 상태
    private static final List<StorageRequestStatus> ACTIVE_STORAGE_REQUEST_STATUSES = List.of(
            StorageRequestStatus.PENDING,
            StorageRequestStatus.SHIPPED_TO_WAREHOUSE,
            StorageRequestStatus.PENDING_INSPECTION,
            StorageRequestStatus.INSPECTING
    );

    @Transactional
    public void withdraw(Long userId, String password, String accessToken) {

        // 1. 사용자 조회
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 본인 확인 (패스워드리스 사용자는 스킵)
        if (!user.getPasswordlessEnabled()) {
            if (password == null || password.isEmpty()) {
                throw new CustomException(ErrorCode.CURRENT_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
        }

        // 3. 탈퇴 불가 조건 검증
        validateWithdraw(userId);

        // 4. OPEN 입찰 자동 취소
        cancelOpenBids(userId);

        // 5. 회원 상태 변경
        user.setStatus(Status.QUITED);
        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now());

        searchService.indexUser(user);  // Typesense 인덱스 갱신

        // 6. JWT 무효화
        String refreshKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(refreshKey);

        long expiration = jwtTokenProvider.getExpiration(accessToken);
        if (expiration > 0) {
            tokenBlacklistService.addToBlacklist(accessToken, expiration);
        }

        log.info("회원 탈퇴 완료: userId={}", userId);
    }

    private void validateWithdraw(Long userId) {
        // 진행 중인 주문 (구매자 또는 판매자)
        if (orderRepository.existsByBuyer_UserIdAndCurrentStatusIn(userId, ACTIVE_ORDER_STATUSES)
                || orderRepository.existsBySeller_UserIdAndCurrentStatusIn(userId, ACTIVE_ORDER_STATUSES)) {
            throw new CustomException(ErrorCode.ACTIVE_ORDER_EXISTS);
        }

        // 미정산 건
        if (settlementRepository.existsBySeller_UserIdAndStatus(userId, SettlementStatus.PENDING)) {
            throw new CustomException(ErrorCode.PENDING_SETTLEMENT_EXISTS);
        }

        // 미납 패널티
        if (userPenaltyRepository.existsByUser_UserIdAndStatus(userId, PenaltyStatus.UNPAID)) {
            throw new CustomException(ErrorCode.UNPAID_PENALTY_EXISTS);
        }

        // 보관 중 상품
        if (storageItemRepository.existsByUser_UserIdAndStatusIn(userId, ACTIVE_STORAGE_STATUSES)) {
            throw new CustomException(ErrorCode.ACTIVE_STORAGE_EXISTS);
        }

        // 진행 중 보관 신청
        if (storageRequestRepository.existsByUser_UserIdAndStatusIn(userId, ACTIVE_STORAGE_REQUEST_STATUSES)) {
            throw new CustomException(ErrorCode.ACTIVE_STORAGE_REQUEST_EXISTS);
        }
    }

    private void cancelOpenBids(Long userId) {
        // 구매 입찰 자동 취소
        List<BuyBid> openBuyBids = buyBidRepository.findByUser_UserIdAndStatus(userId, BidStatus.OPEN);
        for (BuyBid bid : openBuyBids) {
            bid.statusUpdate(BidStatus.CANCELED);
        }

        // 판매 입찰 자동 취소
        List<SaleBid> openSaleBids = saleBidRepository.findByUser_UserIdAndStatus(userId, BidStatus.OPEN);
        for (SaleBid bid : openSaleBids) {
            bid.statusUpdate(BidStatus.CANCELED);
        }

        log.info("OPEN 입찰 자동 취소: userId={}, 구매입찰 {}건, 판매입찰 {}건",
                userId, openBuyBids.size(), openSaleBids.size());
    }
}