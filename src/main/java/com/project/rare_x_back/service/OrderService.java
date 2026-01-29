package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.BidType;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderShippingSnapshotRepository snapshotRepository;
    private final OrderHistoryRepository historyRepository;
    private final AddressRepository addressRepository;
    private final SettlementService settlementService;

    @Transactional
    public Order createOrder(User buyer, User seller, Product product, BuyBid buyBid, SaleBid saleBid, int price, BidType type, Long addressId) {

        // 1. 주문(Order) 저장
        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .buyBid(buyBid)
                .sellBid(saleBid)
                .price(price)
                .type(type)
                .currentStatus(CurrentStatus.PENDING)
                .shipDeadline(LocalDateTime.now().plusDays(2))
                .build();

        Order savedOrder = orderRepository.save(order);

        // 배송지 스냅샷 저장
        saveShippingSnapshot(savedOrder, buyer, addressId);

        // 주문 이력 저장
        saveHistory(savedOrder, CurrentStatus.PENDING);

        return savedOrder;
    }

     // 주문의 상태가 변하는 모든 순간에 호출
     // 상태 변경과 이력 저장을 동시에 처리하는 공통 메서드
    @Transactional
    public void updateOrderStatus(Order order, CurrentStatus newStatus) {
        if (order == null || newStatus == null) {
            return;
        }
        // 1. 상태 업데이트 및 저장
        order.setCurrentStatus(newStatus);
        orderRepository.save(order);

        // 2. 이력 자동 저장
        saveHistory(order, newStatus);
    }

    // 구매 확정 SHIPPED -> DELIVERED
    public void confirmPurchase (Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUserId().equals(buyerId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (order.getCurrentStatus() != CurrentStatus.DELIVERED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "배송 완료 후에만 구매 확정 가능이 가능합니다.");
        }

        // 주문 상태 변경
       // updateOrderStatus(order, CurrentStatus.);

        // 정산 완료 + 지갑 적립..
    }



    // 배송지 스냅샷
    private void saveShippingSnapshot(Order order, User buyer, Long addressId) {
        Address address = addressRepository
                .findByAddressIdAndUser_UserId(addressId, buyer.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "본인의 배송지만 사용할 수 있습니다."));
        snapshotRepository.save(OrderShippingSnapshot.from(order, address));
    }

    // 주문 이력 저장
    private void saveHistory(Order order, CurrentStatus status) {
        historyRepository.save(OrderHistory.create(order, status));
    }
}
