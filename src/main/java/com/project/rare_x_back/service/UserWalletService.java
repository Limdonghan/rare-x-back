package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.UserSettlementHistoryDto;
import com.project.rare_x_back.dto.response.UserWalletAccountResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.SettlementStatus;
import com.project.rare_x_back.enums.WalletHistoryType;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.SettlementRepository;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.repository.UserWalletRepository;
import com.project.rare_x_back.repository.WalletHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserWalletService {

    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;
    private final SettlementRepository settlementRepository;
    private final WalletHistoryRepository walletHistoryRepository;


    // 정산 완료 → 판매자 지갑에 금액 적립
    @Transactional
    public void depositSettlementAmount(Order order, long settleAmount) {
        // 지갑 조회 -> 기존 방식에서 회원 가입 시 빈지갑 생성으로 변경함
        UserWallet wallet = userWalletRepository.findByUser(order.getSeller())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "사용자의 지갑을 찾을 수 없습니다."));

        wallet.increase(settleAmount);

        // 이력 기록
        walletHistoryRepository.save(
                WalletHistory.builder()
                        .wallet(wallet)
                        .amount(settleAmount)
                        .type(WalletHistoryType.SETTLEMENT)
                        .description("판매 정산 완료")
                        .relatedOrderId(order.getOrderId())
                        .build()
        );

    }

    // 지갑 총 액
    @Transactional(readOnly = true)
    public UserWalletAccountResponseDto userWalletAccount(Long userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        UserWallet wallet = userWalletRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "사용자의 지갑 정보를 찾을 수 없습니다."));

        return UserWalletAccountResponseDto.builder()
                .name(user.getName())
                .email(user.getEmail())
                .balance(wallet.getBalance())
                .formattedBalance(String.format("%,d", wallet.getBalance()))
                .build();
    }

    // 지갑에 적립된 정산 내역 (지갑 상세 조회)
    @Transactional(readOnly = true)
    public UserSettlementHistoryDto userSettlementHistory(Long userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserWallet wallet = userWalletRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 최신순으로 조회
        List<Settlement> settlements = settlementRepository.findAllWithOrderAndProduct(userId, SettlementStatus.COMPLETE);

        List<UserSettlementHistoryDto.SettlementItemDto> settlementDtos = settlements.stream()
                .map(s -> UserSettlementHistoryDto.SettlementItemDto.builder()
                        .orderProductName(s.getOrder().getProduct().getProductName()) // 거래 상품
                        .description("판매 정산") // 지갑에 적립된 사유
                        .completedAt(s.getCompletedAt()) // 정산 시점
                        .tradePrice(String.format("%,d원", s.getTotalPrice()))  // 거래체결가격
                        .commissionFee(String.format("-%,d원", s.getCommissionFee()))  // - 수수료
                        .formattedChange(String.format("+%,d원", s.getPayout()))   // 정산되어 더해진 금액, 콤마와 + 기호 추가
                        .build())
                .toList();

        return UserSettlementHistoryDto.builder()
                .userName(user.getName())
                .currentBalance(String.format("%,d원", wallet.getBalance()))
                .settlements(settlementDtos)
                .build();

    }


    // 거래 취소 보상
    @Transactional
    public void compensate(Long userId, long amount, String description, Long orderId) {
        // 방어
        if (amount <= 0) {
            return;
        }

        // 지갑 조회 + 락
        UserWallet wallet = userWalletRepository.findByUserIdForUpdate(userId);

        if (wallet == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 잔액 증가
        wallet.increase(amount);

        // 이력 기록
        walletHistoryRepository.save(
                WalletHistory.builder()
                        .wallet(wallet)
                        .amount(amount)
                        .type(WalletHistoryType.COMPENSATION)
                        .description(description)
                        .relatedOrderId(orderId)
                        .build()
        );
    }



}
