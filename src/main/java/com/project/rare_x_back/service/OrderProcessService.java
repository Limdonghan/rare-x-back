package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.entity.OrderHistory;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.OrderHistoryRepository;
import com.project.rare_x_back.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderProcessService {

    private final OrderRepository orderRepository;
    private final SettlementService settlementService;
    private final OrderHistoryRepository historyRepository;

    // 각 주문마다 완전히 새로운 트랜잭션을 시작.
    // 이렇게 하면 한 주문이 실패해도 다른 주문들은 무사히 커밋됨.
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processIndividualConfirm(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (order.getCurrentStatus() != CurrentStatus.DELIVERED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "배송 완료 상태가 아닙니다.");
        }

        settlementService.completeSettlement(order);

        // 상태 변경 로직 (updateOrderStatus가 OrderService에 있다면 직접 필드 수정)
        order.updateStatus(CurrentStatus.CONFIRMED_PURCHASE);
        order.updateExpAt();
        historyRepository.save(OrderHistory.create(order, CurrentStatus.CONFIRMED_PURCHASE));

    }

}
