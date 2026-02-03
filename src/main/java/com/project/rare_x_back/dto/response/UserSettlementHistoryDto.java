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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementItemDto {
        private String orderProductName; // 거래 상품
        private String description; // 판매 정산 or 기타 적립된 사유 작성
        private LocalDateTime completedAt; // 정산 시점
        private String tradePrice; // 거래 체결 가격
        private String commissionFee; // - 수수료
        private String formattedChange; // 정산 금액
    }
}
