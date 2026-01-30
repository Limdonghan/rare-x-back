package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.BuyingOrderDetailResponseDto;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderShippingSnapshotRepository snapshotRepository;
    private final OrderHistoryRepository historyRepository;
    private final AddressRepository addressRepository;
    private final SearchService searchService;
    private final InspectionRepository inspectionRepository;
    private final PaymentRepository paymentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final SettlementService settlementService;

    private static final int SHIPPING_FEE = 3000;   // 배송비 상수

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

        // Typesense 인덱싱 업데이트
        searchService.indexOrder(order);

        // 2. 이력 자동 저장
        saveHistory(order, newStatus);
    }

    // 구매 확정
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


    private void saveShippingSnapshot(Order order, User buyer, Long addressId) {
        Address address = addressRepository
                .findByAddressIdAndUser_UserId(addressId, buyer.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST, "본인의 배송지만 사용할 수 있습니다."));
        snapshotRepository.save(OrderShippingSnapshot.from(order, address));
    }

    private void saveHistory(Order order, CurrentStatus status) {
        historyRepository.save(OrderHistory.create(order, status));
    }

    // 주문 내역 조회
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
                .orderNumber("ORD-00" + order.getOrderId())
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

    // 주문 상세 조회
    @Transactional(readOnly = true)
    public BuyingOrderDetailResponseDto getBuyingOrderDetail(Long userId, Long orderId) {
        // 1. 주문 조회 + 권한 체크
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 2. 상품 이미지
        List<String> productImages = order.getProduct().getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        // 3. 결제 정보
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElse(null);

        int totalAmount = payment != null ? payment.getAmount() : order.getPrice();
        int productPrice = totalAmount - SHIPPING_FEE;

        // 4. 카드 정보
        String cardCompany = null;
        String cardNumberLast4 = null;

        Optional<BillingKey> billingKey = billingKeyRepository.findByUser(order.getBuyer());
        if (billingKey.isPresent()) {
            cardCompany = billingKey.get().getCardCompany();
            cardNumberLast4 = billingKey.get().getCardNumber();
        }

        // 5. 배송지 정보
        OrderShippingSnapshot snapshot = snapshotRepository.findByOrder_OrderId(orderId)
                .orElse(null);

        // 6. 검수 정보
        String inspectionStatus = null;
        String failReason = null;

        Optional<Inspection> inspection = inspectionRepository.findByOrder_OrderId(orderId);
        if (inspection.isPresent()) {
            inspectionStatus = inspection.get().getStatus().name();
            failReason = inspection.get().getFailReason();
        }

        // 7. 상태 이력 조회
        List<BuyingOrderDetailResponseDto.StatusHistory> statusHistories = historyRepository
                .findByOrder_OrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(history -> BuyingOrderDetailResponseDto.StatusHistory.builder()
                        .status(history.getCurrentStatus().name())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();

        return BuyingOrderDetailResponseDto.builder()
                .orderId(order.getOrderId())
                .orderNumber("ORD-00" + order.getOrderId())
                .createdAt(order.getCreatedAt())
                .currentStatus(order.getCurrentStatus().name())
                .productId(order.getProduct().getProductId())
                .productName(order.getProduct().getProductName())
                .productImages(productImages)
                .productPrice(productPrice)
                .shippingFee(SHIPPING_FEE)
                .totalAmount(totalAmount)
                .cardCompany(cardCompany)
                .cardNumberLast4(cardNumberLast4)
                .recipientName(snapshot != null ? snapshot.getRecipientName() : null)
                .postalCode(snapshot != null ? snapshot.getPostalCode() : null)
                .address(snapshot != null ? snapshot.getAddress() : null)
                .detailAddress(snapshot != null ? snapshot.getDetailAddress() : null)
                .inspectionStatus(inspectionStatus)
                .failReason(failReason)
                .statusHistories(statusHistories)
                .build();
    }
}
