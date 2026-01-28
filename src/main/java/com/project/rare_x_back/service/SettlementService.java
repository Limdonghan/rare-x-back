package com.project.rare_x_back.service;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.entity.Settlement;
import com.project.rare_x_back.enums.SettlementStatus;
import com.project.rare_x_back.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;


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
                .status(SettlementStatus.PENDING)
                .build();

        settlementRepository.save(settlement);
    }


}
