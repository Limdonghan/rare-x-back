package com.project.rare_x_back.service;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.common.PenaltyCalculator;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.*;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
    private final OrderProcessService orderProcessService;
    private final SettlementRepository settlementRepository;
    private final UserWalletService walletService;
    private final SettlementService settlementService;
    private final UserPenaltyRepository userPenaltyRepository;
    private final PaymentService paymentService;


    @Value("${app.service-start-date}")
    private String serviceStartDate;

    @Transactional
    public Order createOrder(User buyer, User seller, Product product, BuyBid buyBid, SaleBid saleBid, int price, BidType type, Long addressId) {

        // 보관상품 판매 여부 확인
        boolean isStorageSale = saleBid != null && saleBid.getStorageItem() != null;

        // 보관상품이면 PASSED, 아니면 PENDING
        CurrentStatus initialStatus = isStorageSale ? CurrentStatus.PASSED : CurrentStatus.PENDING;

        // 보관상품이면 shipDeadline 불필요
        LocalDateTime shipDeadline = isStorageSale ? null : LocalDateTime.now().plusDays(2);

        // 1. 주문(Order) 저장
        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .buyBid(buyBid)
                .sellBid(saleBid)
                .price(price)
                .type(type)
                .currentStatus(initialStatus)
                .shipDeadline(shipDeadline)
                .returnStatus(ReturnStatus.NONE)  // 추가!
                .build();

        Order savedOrder = orderRepository.save(order);

        // 배송지 스냅샷 저장
        saveShippingSnapshot(savedOrder, buyer, addressId);

        // 주문 이력 저장
        saveHistory(savedOrder, initialStatus);

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

        orderProcessService.processIndividualConfirm(orderId);

    }

    // 자동 스케줄링 메서드 (배송 완료 후 5일 이내 구매확정x -> 자동 구매확정)
    public void autoConfirmPurchase() {
        //5일전 시점 계산
        LocalDateTime threshold = LocalDateTime.now().minusDays(5);

        List<Order> orders = orderRepository.findDeliveredOrders(threshold);

        for (Order order : orders) {
            try { // 각 주문마다 완전히 새로운 트랜잭션 시작
                orderProcessService.processIndividualConfirm(order.getOrderId());
                log.info("자동 구매 확정 처리: OrderId = {}", order.getOrderId());
            } catch (Exception e) {
                // 여기서 에러가 나도 다음 루프는 정상 작동하고 이전의 성공건은 커밋됨.
                log.error("주문 {} 처리 중 오류 발생: {}", order.getOrderId(), e.getMessage());
            }
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
                        inspection -> inspection,                            // Value: 검수정보 객체
                        (existing, replacement) -> existing         // 중복키 발생 시 첫번째 유지
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
                .orderNumber(String.format("ORD-%08d", order.getOrderId()))
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
        Payment payment = paymentRepository.findTopByOrder_OrderIdOrderByApprovedAtDesc(orderId)
                .orElse(null);

        int productPrice = order.getPrice();
        int totalAmount = (payment != null) ? payment.getAmount() : FeeCalculator.buyerTotalAmount(productPrice);

        // 4. 결제 방식
        String paymentMethod = (payment != null) ? payment.getMethod() : null;

        // 5. 배송지 정보
        OrderShippingSnapshot snapshot = snapshotRepository.findByOrder_OrderId(orderId)
                .orElse(null);

        // 6. 검수 정보
        String inspectionStatus = null;
        String failReason = null;

        Optional<Inspection> inspection = inspectionRepository.findTopByOrder_OrderIdOrderByCreatedAtDesc(orderId);
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
                .orderNumber(String.format("ORD-%08d", order.getOrderId()))
                .createdAt(order.getCreatedAt())
                .currentStatus(order.getCurrentStatus().name())
                .productId(order.getProduct().getProductId())
                .productName(order.getProduct().getProductName())
                .productImages(productImages)
                .productPrice(productPrice)
                .shippingFee(FeeCalculator.DELIVERY_FEE)
                .totalAmount(totalAmount)
                .paymentMethod(paymentMethod)
                .recipientName(snapshot != null ? snapshot.getRecipientName() : null)
                .postalCode(snapshot != null ? snapshot.getPostalCode() : null)
                .address(snapshot != null ? snapshot.getAddress() : null)
                .detailAddress(snapshot != null ? snapshot.getDetailAddress() : null)
                .inspectionStatus(inspectionStatus)
                .failReason(failReason)
                .statusHistories(statusHistories)
                .build();
    }

    // 판매 내역 조회 (ORDER-006)
    @Transactional(readOnly = true)
    public Page<SellingOrderResponseDto> getSellingOrders(Long userId, String status, Pageable pageable) {
        Page<Order> orders;

        if ("PENDING".equals(status)) {
            // 발송대기
            orders = orderRepository.findBySeller_UserIdAndCurrentStatusIn(
                    userId,
                    List.of(CurrentStatus.PENDING),
                    pageable
            );
        } else if ("INSPECTING".equals(status)) {
            // 검수중
            orders = orderRepository.findBySeller_UserIdAndCurrentStatusIn(
                    userId,
                    List.of(CurrentStatus.SHIPPED_TO_WAREHOUSE, CurrentStatus.PENDING_INSPECTION, CurrentStatus.INSPECTING),
                    pageable
            );
        } else if ("SHIPPING".equals(status)) {
            // 배송중
            orders = orderRepository.findBySeller_UserIdAndCurrentStatusIn(
                    userId,
                    List.of(CurrentStatus.PASSED, CurrentStatus.SHIPPED),
                    pageable
            );
        } else if ("SETTLEMENT_PENDING".equals(status)) {
            // 정산대기
            orders = orderRepository.findBySeller_UserIdAndCurrentStatusIn(
                    userId,
                    List.of(CurrentStatus.DELIVERED),
                    pageable
            );
        } else if ("COMPLETED".equals(status)) {
            // 완료
            orders = orderRepository.findBySeller_UserIdAndCurrentStatusIn(
                    userId,
                    List.of(CurrentStatus.CONFIRMED_PURCHASE),
                    pageable
            );
        } else if ("CANCELLED".equals(status)) {
            // 취소·반송
            orders = orderRepository.findBySeller_UserIdAndCurrentStatusIn(
                    userId,
                    List.of(CurrentStatus.CANCELLED, CurrentStatus.RETURN),
                    pageable
            );
        } else {
            // 전체
            orders = orderRepository.findBySeller_UserId(userId, pageable);
        }

        // 정산 정보 한 번에 조회 (N+1 방지)
        List<Long> orderIds = orders.getContent().stream()
                .map(Order::getOrderId)
                .toList();

        Map<Long, Settlement> settlementMap = settlementRepository.findByOrder_OrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(
                        settlement -> settlement.getOrder().getOrderId(),   // Key: 주문ID
                        settlement -> settlement                            // Value: 정산정보 객체
                ));

        // 검수 정보 한 번에 조회 (N+1 방지)
        Map<Long, Inspection> inspectionMap = inspectionRepository.findByOrder_OrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(
                        inspection -> inspection.getOrder().getOrderId(),     // Key: 주문번호
                        inspection -> inspection,                             // Value: 검수정보 객체
                        (existing, replacement) -> existing          // 중복키 발생 시 첫번째 유지
                ));

        return orders.map(order -> toSellingOrderResponseDto(
                order,
                settlementMap.get(order.getOrderId()),
                inspectionMap.get(order.getOrderId())
        ));
    }

    // 변환 메서드
    private SellingOrderResponseDto toSellingOrderResponseDto(Order order, Settlement settlement, Inspection inspection) {
        String productImage = order.getProduct().getImages().isEmpty()
                ? null
                : order.getProduct().getImages().get(0).getImageUrl();

        Integer settlementPayout = null;
        if (settlement != null) {
            settlementPayout = settlement.getPayout();
        }

        String inspectionFailReason = null;
        if (inspection != null && inspection.getFailReason() != null) {
            inspectionFailReason = inspection.getFailReason();
        }

        return SellingOrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderNumber(String.format("ORD-%08d", order.getOrderId()))
                .createdAt(order.getCreatedAt())
                .productId(order.getProduct().getProductId())
                .productName(order.getProduct().getProductName())
                .productImage(productImage)
                .price(order.getPrice())
                .settlementPayout(settlementPayout)
                .currentStatus(order.getCurrentStatus().name())
                .returnStatus(order.getReturnStatus() != null ? order.getReturnStatus().name() : null)
                .shipDeadline(order.getShipDeadline())
                .inspectionFailReason(inspectionFailReason)
                .build();
    }

    // 판매 상세 조회 (ORDER-007)
    @Transactional(readOnly = true)
    public SellingOrderDetailResponseDto getSellingOrderDetail(Long userId, Long orderId) {
        // 1. 주문 조회 + 권한 체크
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 2. 상품 이미지
        List<String> productImages = order.getProduct().getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        // 3. 정산 정보
        Integer settlementPayout = null;
        LocalDateTime settlementCompletedAt = null;

        Optional<Settlement> settlement = settlementRepository.findByOrder_OrderId(orderId);
        if (settlement.isPresent()) {
            settlementPayout = settlement.get().getPayout();
            settlementCompletedAt = settlement.get().getCompletedAt();
        }

        // 4. 검수 정보
        String inspectionStatus = null;
        String inspectionFailReason = null;

        Optional<Inspection> inspection = inspectionRepository.findTopByOrder_OrderIdOrderByCreatedAtDesc(orderId);
        if (inspection.isPresent()) {
            inspectionStatus = inspection.get().getStatus().name();
            inspectionFailReason = inspection.get().getFailReason();
        }

        // 5. 상태 이력 조회
        List<SellingOrderDetailResponseDto.StatusHistory> statusHistories = historyRepository
                .findByOrder_OrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(history -> SellingOrderDetailResponseDto.StatusHistory.builder()
                        .status(history.getCurrentStatus().name())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();

        return SellingOrderDetailResponseDto.builder()
                .orderId(order.getOrderId())
                .orderNumber(String.format("ORD-%08d", order.getOrderId()))
                .createdAt(order.getCreatedAt())
                .productId(order.getProduct().getProductId())
                .productName(order.getProduct().getProductName())
                .productImages(productImages)
                .price(order.getPrice())
                .settlementPayout(settlementPayout)
                .settlementCompletedAt(settlementCompletedAt)
                .currentStatus(order.getCurrentStatus().name())
                .returnStatus(order.getReturnStatus() != null ? order.getReturnStatus().name() : null)
                .shipDeadline(order.getShipDeadline())
                .sellerShippedAt(order.getSellerShippedAt())
                .inspectionStatus(inspectionStatus)
                .inspectionFailReason(inspectionFailReason)
                .statusHistories(statusHistories)
                .build();
    }

    //=================================== 주문 취소 관련 ===========================================
    // 구매자 주문 취소 (패널티 -> 구매자 = 패널티 제외한 부분 환불)
    @Transactional
    public void cancelByBuyer(Long userId, Long orderId) {
        // 주문 검증
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다."));
        // 구매자인지 검증
        if (!order.getBuyer().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 주문 건만 취소할 수 있습니다.");
        }
        // 이미 취소된 주문인지 확인
        if (order.getCurrentStatus() == CurrentStatus.CANCELLED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "이미 취소된 주문 입니다.");
        }
        // 판매자 발송 전 인지 검증
        if (order.getSellerShippedAt() != null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "판매자가 발송한 주문은 취소할 수 없습니다.");
        }
        //결제 내역 조회
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다."));

        // 구매자 패널티 계산 (거래 체결가의 5%)
        long penalty = PenaltyCalculator.cancelPenalty(order.getPrice());

        // 환불 금액
        long cancelAmount = payment.getAmount() - penalty;

        // 취소 금액 음수 차단
        if (cancelAmount <= 0) {
            throw new CustomException(ErrorCode.INVALID_CANCEL_AMOUNT, "패널티가 결제 금액보다 클 수 없습니다.");
        }

        // 결제 취소 (환불금액 -> 결제 금액에서 - 패널티 차감 금액)
        // 내부에서 markRequested -> API 호출 -> applySuccess 수행
        //이 메서드가 끝나면 결제 취소 기록과 Payment 상태는 이미 DB에 반영
        paymentService.cancelOnce(
                orderId,
                cancelAmount,
                "BUYER_CANCELED",
                "BUYER"
        );

        // 주문, 주문 이력, 패널티 기록, 정산 상태, 판매자 보상 정보 확정 처리
        finalizeBuyerCancellation(order, penalty);

    }

    // 결제 취소 API가 성공한 직후에 이 모든 DB 작업이 한 번에 성공 해야 하므로 따로 뺌.
    private void finalizeBuyerCancellation(Order order, long penalty) {
        // 주문 상태 변경
        order.updateStatus(CurrentStatus.CANCELLED);
        order.updateExpAt();

        // 이력 저장 CANCELED, description 기록
        historyRepository.save(OrderHistory.createCancelHistory(order, CurrentStatus.CANCELLED, "구매자 취소"));

        //패널티 기록
        userPenaltyRepository.save(
                UserPenalty.builder()
                        .user(order.getBuyer())
                        .order(order)
                        .role(PenaltyRole.BUYER)
                        .reason(PenaltyReason.BUYER_CANCEL)
                        .amount(penalty)
                        .status(PenaltyStatus.PAID)
                        .processedAt(LocalDateTime.now())
                        .build()
        );

        // 판매자 보상 (구매자에게 걷은 패널티의 50% -> 판매자 지갑에 적립)
        long compensationAmount = penalty / 2; // 소수점 발생 시 버림 처리
        if (compensationAmount > 0) {
            walletService.compensate(
                    order.getSeller().getUserId(),
                    compensationAmount,
                    "구매자 취소 패널티 보상",
                    order.getOrderId()
            );
        }
        // 정산 상태 변경 (실패 처리)
        settlementService.failSettlement(order.getOrderId(), "ORDER_CANCELED_BY_BUYER");
    }


    // 판매자 주문 취소
    @Transactional
    public void cancelBySeller (Long userId, Long orderId) {
        // 주문 존재 검증
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다."));

        // 판매자인지 검증
        if (!order.getSeller().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 판매 건만 취소할 수 있습니다.");
        }

        // 이미 취소 된 주문인지 확인
        if (order.getCurrentStatus() == CurrentStatus.CANCELLED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "이미 취소된 주문 입니다.");
        }

        // 발송 여부 검증
        if (order.getSellerShippedAt() != null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "발송한 주문은 취소 할 수 없습니다.");
        }

        // 구매자 결제 내역 조회
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND, "구매자의 결제 내역을 찾을 수 없습니다."));

        // 환불 금액 (전액 -> 판매자 취소이므로)
        long cancelAmount = payment.getAmount();
        // 구매자 환불 (전액)
        paymentService.cancelOnce(
                orderId,
                cancelAmount,
                "SELLER_CANCELED",
                "SELLER"
        );

        // 주문, 주문 이력, 정산 상태 fail 처리
        finalizeSellerCancellation(order);

    }

    // 결제 취소 API가 성공한 직후에 이 모든 DB 작업이 한 번에 성공 해야 하므로 따로 뺌.
    private void finalizeSellerCancellation(Order order) {
        // 주문 상태 변경
        order.updateStatus(CurrentStatus.CANCELLED);
        order.updateExpAt();

        // 이력 저장 CANCELED, description 기록
        historyRepository.save(OrderHistory.createCancelHistory(order, CurrentStatus.CANCELLED, "판매자 취소"));

        // 정산 상태 변경 (실패 처리)
        settlementService.failSettlement(order.getOrderId(), "ORDER_CANCELED_BY_SELLER");
    }

    //==============================================================================



    // ====== 관리자 주문 목록 조회 (MANAGER-009) ======
    @Transactional(readOnly = true)
    public Page<AdminOrderResponseDto> getAdminOrders(
            List<String> status, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {

        // DB 직접 조회
        List<CurrentStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            statuses = status.stream()
                    .map(s -> {
                        try {
                           return CurrentStatus.valueOf(s);
                        } catch (IllegalArgumentException e) {
                            throw new CustomException(ErrorCode.BAD_REQUEST);
                        }
                    })
                    .toList();
        }

        // 날짜 한쪽만 입력된 경우 보정 ( startDate 의 경우 서비스 시작일(임시))
        if (startDate != null && endDate == null) {
            endDate = LocalDateTime.now();
        }
        if (endDate != null && startDate == null) {
            startDate = LocalDate.parse(serviceStartDate).atStartOfDay();
        }

        Page<Order> orders;

        if (statuses != null && startDate != null) {
            orders = orderRepository.findByCurrentStatusInAndCreatedAtBetween(
                    statuses, startDate, endDate, pageable);
        } else if (statuses != null) {
            orders = orderRepository.findByCurrentStatusIn(statuses, pageable);
        } else if (startDate != null) {
            orders = orderRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else {
            orders = orderRepository.findAllForAdmin(pageable);
        }

        return orders.map(this::toAdminOrderResponseDto);
    }

    private AdminOrderResponseDto toAdminOrderResponseDto(Order order) {
        return AdminOrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderNumber(String.format("ORD-%08d", order.getOrderId()))
                .createdAt(order.getCreatedAt())
                .buyerName(order.getBuyer().getName())
                .sellerName(order.getSeller().getName())
                .productName(order.getProduct().getProductName())
                .price(order.getPrice())
                .currentStatus(order.getCurrentStatus().name())
                .build();
    }

    // ====== 관리자 주문 상세 조회 (MANAGER-009) ======
    @Transactional(readOnly = true)
    public AdminOrderDetailResponseDto getAdminOrderDetail(Long orderId) {
        // 1. 주문 조회 (buyer, seller, product, images 한방 로딩)
        Order order = orderRepository.findAdminOrderDetail(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 상품 이미지
        List<String> productImages = order.getProduct().getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        // 3. 결제 정보 (가장 최근 결제 내역)
        Payment payment = paymentRepository.findTopByOrder_OrderIdOrderByApprovedAtDesc(orderId)
                .orElse(null);

        // 4. 정산 정보 (판매자에게 지급된 금액 등)
        Settlement settlement = settlementRepository.findByOrder_OrderId(orderId)
                .orElse(null);

        // 5. 배송지 정보
        OrderShippingSnapshot snapshot = snapshotRepository.findByOrder_OrderId(orderId)
                .orElse(null);

        // 6. 검수 정보 (가장 최근 검수 내역)
        Inspection inspection = inspectionRepository.findTopByOrder_OrderIdOrderByCreatedAtDesc(orderId)
                .orElse(null);

        // 7. 상태 이력
        List<AdminOrderDetailResponseDto.StatusHistory> statusHistories = historyRepository
                .findByOrder_OrderIdOrderByCreatedAtAsc(orderId)    // 오래된 순으로 조회
                .stream()
                .map(history -> AdminOrderDetailResponseDto.StatusHistory.builder()
                        .status(history.getCurrentStatus().name())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();

        // 최종적으로 DTO 객체를 만들어 반환
        return AdminOrderDetailResponseDto.builder()
                // 기본
                .orderId(order.getOrderId())
                .orderNumber(String.format("ORD-%08d", order.getOrderId()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                // 구매자
                .buyerName(order.getBuyer().getName())
                .buyerEmail(order.getBuyer().getEmail())
                // 판매자
                .sellerName(order.getSeller().getName())
                .sellerEmail(order.getSeller().getEmail())
                // 상품
                .productId(order.getProduct().getProductId())
                .productName(order.getProduct().getProductName())
                .productImages(productImages)
                .brandName(order.getProduct().getBrand().getBrandName())
                // 거래
                .price(order.getPrice())
                .bidType(order.getType() != null ? order.getType().name() : null)
                .currentStatus(order.getCurrentStatus().name())
                .returnStatus(order.getReturnStatus() != null ? order.getReturnStatus().name() : null)
                // 발송
                .shipDeadline(order.getShipDeadline())
                .sellerShippedAt(order.getSellerShippedAt())
                // 결제
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .paymentAmount(payment != null ? payment.getAmount() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                // 정산
                .settlementPayout(settlement != null ? settlement.getPayout() : null)
                .settlementStatus(settlement != null ? settlement.getStatus().name() : null)
                .settlementCompletedAt(settlement != null ? settlement.getCompletedAt() : null)
                // 배송지
                .recipientName(snapshot != null ? snapshot.getRecipientName() : null)
                .postalCode(snapshot != null ? snapshot.getPostalCode() : null)
                .address(snapshot != null ? snapshot.getAddress() : null)
                .detailAddress(snapshot != null ? snapshot.getDetailAddress() : null)
                // 검수
                .inspectionStatus(inspection != null ? inspection.getStatus().name() : null)
                .inspectionFailReason(inspection != null ? inspection.getFailReason() : null)
                // 이력
                .statusHistories(statusHistories)
                .build();
    }
}
