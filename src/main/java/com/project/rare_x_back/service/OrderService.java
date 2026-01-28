package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.BuyingOrderResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.BidType;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderShippingSnapshotRepository snapshotRepository;
    private final OrderHistoryRepository historyRepository;
    private final AddressRepository addressRepository;
    private final SearchService searchService;
    private final InspectionRepository inspectionRepository;

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
                .build();
        Order savedOrder = orderRepository.save(order);

        // 2. 배송지 스냅샷 저장
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, buyer.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "본인의 배송지만 사용할 수 있습니다."));
        snapshotRepository.save(OrderShippingSnapshot.from(savedOrder, address));

        // 3. 주문 이력 저장
        historyRepository.save(OrderHistory.create(savedOrder, savedOrder.getCurrentStatus()));

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

        // Typesense 인덱싱 업데이트
        searchService.indexOrder(order);

        // 2. 이력 자동 저장
        addOrderHistory(order, newStatus);
    } //이력 저장을 담당하는 내부 메서드

    private void addOrderHistory(Order order, CurrentStatus status) {
        historyRepository.save(OrderHistory.create(order, status));
    }

    // 구매 내역 조회
    @Transactional(readOnly = true)
    public Page<BuyingOrderResponseDto> getBuyingOrders(Long userId, String status, Pageable pageable) {
        Page<Order> orders;

        if ("IN_PROGRESS".equals(status)) {
            List<CurrentStatus> statuses = List.of(
                    CurrentStatus.PENDING,
                    CurrentStatus.SHIPPED_TO_WAREHOUSE,
                    CurrentStatus.PENDING_INSPECTION,
                    CurrentStatus.INSPECTING,
                    CurrentStatus.PASSED,
                    CurrentStatus.SHIPPED
            );
            orders = orderRepository.findByBuyer_UserIdAndCurrentStatusIn(userId, statuses, pageable);

        } else if ("COMPLETED".equals(status)) {
            List<CurrentStatus> statuses = List.of(
                    CurrentStatus.DELIVERED,
                    CurrentStatus.RETURN,
                    CurrentStatus.CANCELLED
            );
            orders = orderRepository.findByBuyer_UserIdAndCurrentStatusIn(userId, statuses, pageable);

        } else {
            orders = orderRepository.findByBuyer_UserId(userId, pageable);
        }

        // 검수 정보 한 번에 조회 (N+1 방지)
        List<Long> orderIds = orders.getContent().stream()
                .map(Order::getOrderId)
                .toList();

        Map<Long, Inspection> inspectionMap = inspectionRepository.findByOrder_OrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(
                        inspection -> inspection.getOrder().getOrderId(),   // Key: 주문번호
                        inspection -> inspection                            // Value: 검수정보 객체
                ));

        return orders.map(order -> toBuyingOrderResponseDto(order, inspectionMap.get(order.getOrderId())));
    }

    // 변환 메서드 수정
    private BuyingOrderResponseDto toBuyingOrderResponseDto(Order order, Inspection inspection) {
        String productImage = order.getProduct().getImages().isEmpty()
                ? null
                : order.getProduct().getImages().get(0).getImageUrl();

        String inspectionStatus = null;
        String inspectionFailReason = null;

        if (inspection != null) {
            inspectionStatus = inspection.getStatus().name();
            inspectionFailReason = inspection.getFailReason();
        }

        return BuyingOrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderNumber("ORD-" + order.getOrderId())
                .createdAt(order.getCreatedAt())
                .productId(order.getProduct().getProductId())
                .productName(order.getProduct().getProductName())
                .productImage(productImage)
                .price(order.getPrice())
                .currentStatus(order.getCurrentStatus().name())
                .inspectionStatus(inspectionStatus)
                .inspectionFailReason(inspectionFailReason)
                .build();
    }
}
