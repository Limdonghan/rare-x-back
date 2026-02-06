package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSettlementHistoryDto {
    private String userName; // 유저이름
    private String currentBalance; // 현재 지갑 총액
    private List<SettlementItemDto> settlements;
    private List<CompensationItemDto> compensationItems;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementItemDto {
        private String orderProductName; // 거래 상품
        private String description; // 사유 - 판매 정산
        private LocalDateTime completedAt; // 정산 시점
        private String tradePrice; // 거래 체결 가격
        private String commissionFee; // - 수수료
        private String formattedChange; // 정산 금액
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompensationItemDto {
        private String description; // 사유 - 거래 취소 패널티 보상
        private String compensationAmount;
    }

}
