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

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderShippingSnapshotRepository snapshotRepository;
    private final OrderHistoryRepository historyRepository;
    private final AddressRepository addressRepository;

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
                .currentStatus(CurrentStatus.PENDING_INSPECTION)
                .build();
        Order savedOrder = orderRepository.save(order);

        // 2. 배송지 스냅샷 저장
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "배송지 정보가 없습니다."));
        snapshotRepository.save(OrderShippingSnapshot.from(savedOrder, address));

        // 3. 주문 이력 저장
        historyRepository.save(OrderHistory.create(savedOrder, savedOrder.getCurrentStatus()));

        return savedOrder;
    }
}