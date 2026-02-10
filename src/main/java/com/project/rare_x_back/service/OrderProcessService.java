package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.entity.OrderHistory;
import com.project.rare_x_back.entity.Payment;
import com.project.rare_x_back.enums.CancelStatus;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.OrderHistoryRepository;
import com.project.rare_x_back.repository.OrderRepository;
import com.project.rare_x_back.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessService {

    private final OrderRepository orderRepository;
    private final SettlementService settlementService;
    private final OrderHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

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

    @Transactional
    public void cancelDueToShipDeadline(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (order.getCurrentStatus() == CurrentStatus.CANCELLED) {
            log.debug("이미 취소된 주문 skip: {}", orderId);
            return;
        }

        if (order.getSellerShippedAt() != null) {
            log.debug("이미 발송한 주문 skip: {}", orderId);
            return;
        }

        if (order.getExpiresAt() != null) {
            log.debug("이미 만료된 주문 skip: {}", orderId);
            return;
        }

        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등 체크 추가
        if (payment.getCancelStatus() == CancelStatus.CANCELED) {
            log.debug("이미 취소된 결제 skip : orderId = {}", orderId);
            return;
        }

        if (payment.getCancelStatus() == CancelStatus.REQUESTED) {
            log.debug("이미 취소 요청 중 skip : orderId = {}", orderId);
            return;
        }
        paymentService.cancelOnce(
                orderId,
                payment.getAmount(),
                "SELLER_CANCELED",
                "SELLER"
        );

        finalizeSellerCancellation(order);
    }

    private void finalizeSellerCancellation(Order order) {
        order.updateStatus(CurrentStatus.CANCELLED);
        order.updateExpAt();

        historyRepository.save(
                OrderHistory.createCancelHistory(order, CurrentStatus.CANCELLED, "판매자 취소")
        );

        settlementService.failSettlement(order.getOrderId(), "ORDER_CANCELED_BY_SELLER");
    }
}


