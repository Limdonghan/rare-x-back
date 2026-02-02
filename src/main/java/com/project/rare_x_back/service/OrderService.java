package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.BidType;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.AddressRepository;
import com.project.rare_x_back.repository.OrderHistoryRepository;
import com.project.rare_x_back.repository.OrderRepository;
import com.project.rare_x_back.repository.OrderShippingSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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


    // 구매 확정 DELIVERED -> CONFIRM_PURCHASE
    @Transactional
    public void confirmPurchase (Order order) {

        // 주문 상태가 배송완료인지 확인
        if (order.getCurrentStatus() != CurrentStatus.DELIVERED) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // 주문 상태 변경
        updateOrderStatus(order, CurrentStatus.CONFIRMED_PURCHASE);

        // 정산 상태 완료 변경
        settlementService.completeSettlement(order);
    }

    // 유저 -> 구매확정
    @Transactional
    public void userConfirmPurchase(Long orderId, Long buyerId) {
        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다."));

        // 구매자가 아니면 권한 없음
        if (!order.getBuyer().getUserId().equals(buyerId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        confirmPurchase(order);

    }


    // 자동 스케줄링 메서드 (배송 완료 후 5일 이내 구매확정x -> 자동 구매확정)
    @Transactional
    public void autoConfirmPurchase() {
        List<Order> orders = orderRepository.findDeliveredOrder(LocalDateTime.now().minusDays(5));

        for (Order order : orders) {
            confirmPurchase(order); // 공통 메서드
        }

    }


    // 관리자 주문 배송 완료로 상태 변경 (AdminController) SHIPPED -> DELIVERED
    @Transactional
    public void deliveryComplete (Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다."));

        //2. 상태가 검수 통과 후 발송한 상태인지 확인
        if (order.getCurrentStatus() != CurrentStatus.SHIPPED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "발송된 주문이 아닙니다.");
        }
        // 주문 상태 변경 및 주문 이력 저장
        updateOrderStatus(order, CurrentStatus.DELIVERED);
    }
}
