package com.project.rare_x_back.service;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.entity.Settlement;
import com.project.rare_x_back.enums.SettlementStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserWalletService userWalletService;

    @Transactional
    public void createSettlement(Order order) {

        int commission = FeeCalculator.sellerFee(order.getPrice());
        int payout = FeeCalculator.sellerPayout(order.getPrice());

        Settlement settlement = Settlement.builder()
                .order(order)
                .seller(order.getSeller())
                .sellBid(order.getSellBid())
                .commissionFee(commission)
                .payout(payout)
                .totalPrice(order.getPrice())
                .status(SettlementStatus.PENDING)
                .build();

        settlementRepository.save(settlement);
    }


    @Transactional
    public void completeSettlement(Order order) {

        Settlement settlement = settlementRepository
                .findByOrder_OrderId(order.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "정산 정보를 찾을 수 없습니다."));

        // 이미 정산 완료면 차단
        if (settlement.getStatus() == SettlementStatus.COMPLETE) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "이미 완료된 정산 입니다.");
        }

        // Long sellerId = order.getSeller().getUserId();
        int settleAmount = settlement.getPayout(); // 수수료 제외 금액

        // 정산 상태 완료 처리
        settlement.complete();

        // 판매자 지갑 적립
        userWalletService.depositSettlementAmount(order.getSeller(), settleAmount);


    }
}
