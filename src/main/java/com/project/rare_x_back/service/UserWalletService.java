package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.UserSettlementHistoryDto;
import com.project.rare_x_back.dto.response.UserWalletAccountResponseDto;
import com.project.rare_x_back.entity.Settlement;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.entity.UserWallet;
import com.project.rare_x_back.enums.SettlementStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.OrderRepository;
import com.project.rare_x_back.repository.SettlementRepository;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.repository.UserWalletRepository;
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
    private final OrderRepository orderRepository;


    // 정산 완료 → 판매자 지갑에 금액 적립
    @Transactional
    public void depositSettlementAmount(User seller, long settleAmount) {
        // 지갑 조회 -> 기존 방식에서 회원 가입 시 빈지갑 생성으로 변경함
        UserWallet wallet = userWalletRepository.findByUser(seller)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "사용자의 지갑을 찾을 수 없습니다."));

        wallet.increase(settleAmount);
    }

    // 지갑 총 액
    @Transactional(readOnly = true)
    public UserWalletAccountResponseDto userWalletAccount(Long userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND,"사용자를 찾을 수 없습니다."));

        UserWallet wallet = userWalletRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND,"사용자의 지갑 정보를 찾을 수 없습니다."));

        return UserWalletAccountResponseDto.builder()
                .name(user.getName())
                .email(user.getEmail())
                .balance(wallet.getBalance())
                .formattedBalance(String.format("%,d", wallet.getBalance()))
                .build();
    }

    // 지갑에 적립된 정산 내력 (지갑 상세 조회?)
    @Transactional(readOnly = true)
    public List<UserSettlementHistoryDto> userSettlementHistory(Long userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserWallet wallet = userWalletRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 최신순으로 조회
        List<Settlement> settlements = settlementRepository.findAllBySeller_UserIdAndStatusOrderByCompletedAtDesc(userId, SettlementStatus.COMPLETE);

        List<UserSettlementHistoryDto.SettlementItemDto> settlementDtos = settlements.stream()
                .map(s -> UserSettlementHistoryDto.SettlementItemDto.builder()
                        .orderProductName(s.getOrder().getProduct().getProductName())
                        .description("판매 정산")
                        .completedAt(s.getCompletedAt())
                        .formattedChange(String.format("+%,d원", s.getPayout())) // 정산되어 더해진 금액, 콤마와 + 기호 추가
                        .build())
                .toList();

        return List.of(UserSettlementHistoryDto.builder()
                .userName(user.getName())
                .currentBalance(wallet.getBalance())
                .settlements(settlementDtos)
                .build());

    }

}
