package com.project.rare_x_back.service;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.*;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BidService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BuyBidRepository buyBidRepository;
    private final SaleBidRepository saleBidRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final AddressRepository addressRepository;
    private final StorageItemRepository storageItemRepository;
    private final InspectionRepository inspectionRepository;
    @Value("${inspection-center.address}")
    private String inspectionCenterAddress;
    @Value("${inspection-center.zipcode}")
    private String inspectionCenterZipcode;

    private final OrderService orderService;
    private final SearchService searchService;

    public FeeResponseDto getFees() {
        return FeeResponseDto.builder()
                .buyerFeeRate(FeeCalculator.BUYER_FEE_RATE)
                .sellerFeeRate(FeeCalculator.SELLER_FEE_RATE)
                .deliveryFee(FeeCalculator.DELIVERY_FEE)
                .build();
    }

    /**
     * [판매 입찰 등록]
     * 1. 판매자가 상품을 등록 (완료)
     * 1 -1. 상품 상세에서 판매 입찰 등록
     * 1 -2. 보관 중 상품에서 상품 등록
     * 2. 구매자가 있으면 즉시체결 OR 자동결제 -> 매칭이되면
     *
     */
    @Transactional
    public RegisterSaleBidResponseDto registerSaleBid(RegisterSaleBidRequestDto registerSaleBidRequestDto, String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(registerSaleBidRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 빌링키 존재 여부 검증 -> 판매 입찰 시에는 패널티 징수용 보증 수단
        paymentService.validateBillingKey(user.getUserId());

        Optional<StorageItem> storageItemOpt =
                storageItemRepository
                        .findFirstByUser_UserIdAndProduct_ProductIdAndStatusOrderByExpiredAtAsc(
                                user.getUserId(),
                                product.getProductId(),
                                StorageStatus.STORED
                        ); // -> 있으면  storageItemOpt.isPresent(); = true

        /// [추가] 만약 보관 중인 상품의 입찰일 경후 보관 상품 상태 변경
        boolean storageItemCheck = storageItemOpt.isPresent();  // -> true 일때  StorageItem = 위에서 찾은 재고
        if (storageItemCheck){
            storageItemOpt.get().updateStatus(StorageStatus.ON_SALE);
        }
        StorageItem storageItem = storageItemOpt.orElse(null);  // 없으면 StorageItem = null

        SaleBid build = SaleBid.builder()
                .user(user)
                .product(product)
                .price(registerSaleBidRequestDto.getPrice())
                .status(BidStatus.OPEN)
                .storageItem(storageItem)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        SaleBid savedBid = saleBidRepository.save(build);

        attemptMatchForSaleBid(savedBid);

        return RegisterSaleBidResponseDto.builder()
                .bidId(savedBid.getSellId())
                .productName(product.getProductName())
                .brandName(product.getBrand().getBrandName())
                .category(product.getCategory().getCategoryName())
                .price(registerSaleBidRequestDto.getPrice())
                .status(storageItemCheck)
                .build();
    }


    /**
     * [구매 입찰 등록]
     * 1. 구매자가 입츨을 등록 (완료)
     * 2. 판매자가 있으면 즉시체결 OR 자동결제
     *
     */
    @Transactional
    public RegisterBuyBidResponseDto registerBuyBid(RegisterBuyBidRequestDto registerBuyBidRequestDto, String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(registerBuyBidRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        // 주소 존재 여부 및 해당 사용자 소유 여부를 검증하기 위한 조회 (엔티티 자체는 이후 사용하지 않음)
        Address address = addressRepository.findByAddressIdAndUser_UserId(registerBuyBidRequestDto.getAddressId(), user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST));

        // 빌링키 존재 여부 검증
        paymentService.validateBillingKey(user.getUserId());

        BuyBid build = BuyBid.builder()
                .user(user)
                .product(product)
                .addressId(registerBuyBidRequestDto.getAddressId())
                .price(registerBuyBidRequestDto.getPrice())
                .status(BidStatus.OPEN)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        BuyBid savedBid = buyBidRepository.save(build);

        attemptMatchForBuyBid(savedBid);

        return RegisterBuyBidResponseDto.builder()
                .bidId(savedBid.getBuyId())
                .productName(product.getProductName())
                .brandName(product.getBrand().getBrandName())
                .category(product.getCategory().getCategoryName())
                .price(registerBuyBidRequestDto.getPrice())
                .build();
    }

    /**
     * [즉시 구매 결제 처리]
     * 1. 구매자가 해당 상품 선택
     * 2. 주문 테이블 생성
     * 3. 입찰 상태 변경
     * 4. 결제 시도
     *
     */
    @Transactional
    public PurchaseResponseDto purchaseNow(PurchaseRequestDto purchaseRequestDto, String email) {
        User buyer = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(purchaseRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        /// [매칭] 해당 가격에 파는 판매 입찰(SaleBid) 찾기, (가장 저렴하고, 먼저 등록된 판매 입찰 1개 조회 + 본인 입찰 제외 추가)
        List<SaleBid> saleBidList = saleBidRepository.findAllByProductAndPriceAndStatusAndUserNot(
                product,
                purchaseRequestDto.getPrice(),
                BidStatus.OPEN,
                buyer.getUserId(),
                PageRequest.of(0, 1)
        );

        if (saleBidList.isEmpty()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        SaleBid saleBid = saleBidList.getFirst();

        if (saleBid.getUser().getUserId().equals(buyer.getUserId())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "본인의 판매 입찰은 구매할 수 없습니다.");
        }

        /// [상태 변경] 판매 입찰 -> 체결됨(MATCHED)
        saleBid.statusUpdate(BidStatus.MATCHED);

        /// [주문 생성] Order 만들기 -> OrderService 추가 후 리팩터링
        Order order = orderService.createOrder(
                buyer,                                  // 구매자
                saleBid.getUser(),                      // 판매자
                product,                                // 상품
                null,                                   // buyBid -> 즉시 구매는 구매 입찰이 없음
                saleBid,                                // sellBid
                purchaseRequestDto.getPrice(),          // 구매가격
                BidType.BUY,                            // 체결 타입
                purchaseRequestDto.getAddressId()
        );

        // 보관 상품 즉시 구매 체결 시 StorageItem → SOLD 전환
        // LAZY 로딩 문제를 피하기 위해 Repository에서 직접 조회
        storageItemRepository.findBySellBidId(saleBid.getSellId()).ifPresent(storageItem -> {
            storageItem.updateStatus(StorageStatus.SOLD);
            log.info("즉시 구매 체결: SaleBidId={}, StorageItem SOLD 처리 완료", saleBid.getSellId());
        });

        /// 결제 승인
        PaymentConfirmRequestDto paymentConfirmRequestDto = PaymentConfirmRequestDto.builder()
                .paymentKey(purchaseRequestDto.getPaymentKey())
                .tossOrderId(purchaseRequestDto.getTossOrderId())
                .amount(purchaseRequestDto.getAmount())
                .orderId(order.getOrderId())
                .build();
        try {
            paymentService.confirmPayment(paymentConfirmRequestDto, email);

        } catch (Exception e) {
            log.error("결제 승인 실패 주문: {} , 사용자 {}.", order.getOrderId(), email, e);
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        return PurchaseResponseDto.builder()
                .productName(product.getProductName())
                .brandName(product.getBrand().getBrandName())
                .category(product.getCategory().getCategoryName())
                .tossOrderId(purchaseRequestDto.getTossOrderId())
                .amount(purchaseRequestDto.getAmount())
                .build();

    }


    /**
     * [Order 발송 처리]
     * 판매자가 검수센터로 상품 발송 완료 처리
     * PENDING → SHIPPED_TO_WAREHOUSE + Inspection 생성
     */
    @Transactional
    public OrderShipResponseDto shipOrderToWarehouse(String email, Long orderId) {
        // 1. 사용자 조회
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. Order 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문을 찾을 수 없습니다."));

        // 3. 본인 확인 (판매자만 발송 가능)
        if (!order.getSeller().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 판매 건만 발송 처리할 수 있습니다.");
        }

        // 4. 상태 확인 (PENDING만 발송 가능)
        if (order.getCurrentStatus() != CurrentStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "발송 대기 상태에서만 발송 처리가 가능합니다.");
        }

        if (LocalDateTime.now().isAfter(order.getShipDeadline())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "발송 마감 기한이 지났습니다.");
        }

        // 5-1 판매자 -> 검수센터 발송 완료 시간 기록
        order.updateToShipped();
        // 5-2 Order 상태 변경 + 주문 이력 저장
        orderService.updateOrderStatus(order, CurrentStatus.SHIPPED_TO_WAREHOUSE);
        
        // 6. Inspection 생성
        Inspection inspection = Inspection.builder()
                .order(order)
                .type(InspectionType.ORDER)
                .status(InspectionStatus.SHIPPED_TO_WAREHOUSE)
                .build();
        inspectionRepository.save(inspection);
        searchService.indexInspection(inspection);

        return OrderShipResponseDto.from(order, inspectionCenterAddress, inspectionCenterZipcode);
    }

    /**
     * [즉시 판매 결제 처리]
     * 1. 구매자가 해당 상품 선택
     * 2. 주문 테이블 생성
     * 3. 입찰 상태 변경
     * 4. 결제 시도
     *
     */
    @Transactional
    public SellNowResponseDto sellNow(SellNowRequestDto sellNowRequestDto, String email) {

        ///  판매자 조회
        User seller = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(sellNowRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        BuyBid buyBid = buyBidRepository.findForSellNow(
                sellNowRequestDto.getBidId(),
                BidStatus.OPEN,
                seller.getUserId()
        ).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_ON_BID));

        if (buyBid.getUser().getUserId().equals(seller.getUserId())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "본인의 구매 입찰은 판매할 수 없습니다.");
        }

        String orderNumber = UUID.randomUUID().toString();

        /// [상태 변경] 구매입찰 -> 체결됨
        buyBid.statusUpdate(BidStatus.MATCHED);

        Order order = orderService.createOrder(
                buyBid.getUser(),
                seller,
                product,
                buyBid,
                null,
                sellNowRequestDto.getPrice(),
                BidType.SELL,
                buyBid.getAddressId()
        );

        Order saveOrder = orderRepository.save(order);

        AutoPaymentRequestDto autoPaymentRequestDto = AutoPaymentRequestDto.builder()
                .userId(buyBid.getUser().getUserId())
                .orderId(saveOrder.getOrderId())
                .amount(saveOrder.getPrice())
                .tossOrderId(orderNumber)
                .orderName(product.getProductName())
                .build();

        try {
            paymentService.payWithBillingKey(autoPaymentRequestDto);
        } catch (Exception e) {
            log.error("즉시 판매 결제 실패: 주문번호 {}, 에러: {}", orderNumber, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
        return SellNowResponseDto.builder()
                .tossOrderId(orderNumber)
                .productName(product.getProductName())
                .brandName(product.getBrand().getBrandName())
                .category(product.getCategory().getCategoryName())
                .amount(saveOrder.getPrice())
                .build();

    }

    // 자신의 구매입찰 내역 조회(마이페이지에서)
    @Transactional(readOnly = true)
    public List<MyBuyBidResponseDto> getMyBuyBids(String email, BidStatus status) {

        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 입찰 조회 (status 있으면 필터, 없으면 전체)
        List<BuyBid> bids;

        if (status == null) {
            bids = buyBidRepository.findMyBuyBidsAll(user.getUserId());
        } else {
            bids = buyBidRepository.findMyBuyBidsByStatus(user.getUserId(), status);
        }

        // DTO 변환
        return bids.stream()
                .map(MyBuyBidResponseDto::from)
                .toList();
    }

    // 판매입찰 조회
    @Transactional(readOnly = true)
    public List<MySaleBidResponseDto> getMySaleBids(String email, BidStatus status) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 입찰 조회 (status 있으면 필터, 없으면 전체)
        List<SaleBid> bids;
        if (status == null) {
            bids = saleBidRepository.findMySaleBidsAll(user.getUserId());
        } else {
            bids = saleBidRepository.findMySaleBidsByStatus(user.getUserId(), status);
        }

        return bids.stream()
                .map(MySaleBidResponseDto::from)
                .toList();
    }

    // 판매 입찰 체결됨 탭 조회 (Order 기반)
    @Transactional(readOnly = true)
    public List<MySaleBidMatchedResponseDto> getMySaleBidMatched(String email, String orderStatusStr) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Order> orders;
        if (orderStatusStr == null || orderStatusStr.trim().isEmpty()) {
            orders = orderRepository.findBySeller_UserId(user.getUserId(),
                    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        } else {
            List<CurrentStatus> statuses = parseAndMapToStatuses(orderStatusStr);
            // BEFORE_SHIPPING (PASSED)의 경우, 통계 로직과 동일하게 분리할 수도 있지만
            // 여기선 기존 로직이 'findBySeller_UserIdAndCurrentStatusIn' 이므로 
            // 상태값 목록으로 조회하게 매핑된statuses를 그대로 사용
            orders = orderRepository.findBySeller_UserIdAndCurrentStatusIn(
                    user.getUserId(), statuses,
                    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        }

        return orders.stream()
                .map(MySaleBidMatchedResponseDto::from)
                .toList();
    }

    // 구매 입찰 체결됨 탭 조회 (Order 기반)
    @Transactional(readOnly = true)
    public List<MyBuyBidMatchedResponseDto> getMyBuyBidMatched(String email, String orderStatusStr) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Order> orders;
        if (orderStatusStr == null || orderStatusStr.trim().isEmpty()) {
            orders = orderRepository.findByBuyer_UserId(user.getUserId(),
                    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        } else {
            List<CurrentStatus> statuses = parseAndMapToStatuses(orderStatusStr);
            orders = orderRepository.findByBuyer_UserIdAndCurrentStatusIn(
                    user.getUserId(), statuses,
                    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        }

        return orders.stream()
                .map(MyBuyBidMatchedResponseDto::from)
                .toList();
    }

    // CurrentStatus 매핑 (통계 탭 별 상태 그룹핑)
    private List<CurrentStatus> parseAndMapToStatuses(String filterStatusStr) {
        if ("BEFORE_SHIPPING".equals(filterStatusStr)) {
            return List.of(CurrentStatus.PASSED);
        }
        
        CurrentStatus filterStatus;
        try {
            filterStatus = CurrentStatus.valueOf(filterStatusStr);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "유효하지 않은 주문 상태입니다.");
        }

        return switch (filterStatus) {
            case PENDING -> List.of(CurrentStatus.PENDING);
            case INSPECTING -> List.of(
                    CurrentStatus.SHIPPED_TO_WAREHOUSE,
                    CurrentStatus.PENDING_INSPECTION,
                    CurrentStatus.INSPECTING
            );
            case PASSED -> List.of(CurrentStatus.PASSED);
            case SHIPPED -> List.of(CurrentStatus.SHIPPED); // 순수 배송중
            case DELIVERED -> List.of(CurrentStatus.DELIVERED);
            case CONFIRMED_PURCHASE -> List.of(CurrentStatus.CONFIRMED_PURCHASE);
            case CANCELLED -> List.of(CurrentStatus.RETURN, CurrentStatus.CANCELLED);
            default -> List.of(filterStatus);
        };
    }

    // 구매 입찰 가격 수정
    @Transactional
    public void updateBuyBidPrice(Long userId, Long buyId, UpdateBidPriceRequestDto dto) {
        BuyBid updateBid = buyBidRepository.findByBuyIdAndUser_UserId(buyId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        if (updateBid.getStatus() != BidStatus.OPEN) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "매칭 대기 중인 입찰만 수정할 수 있습니다.");
        }
        updateBid.buyPriceUpdate(dto.getPrice());
        // 가격 수정 시에도 매칭 돌려봄
        attemptMatchForBuyBid(updateBid);
    }

    // 판매 입찰 가격 수정
    @Transactional
    public void updateSaleBidPrice(Long userId, Long sellId, UpdateBidPriceRequestDto dto) {
        SaleBid updateBid = saleBidRepository.findBySellIdAndUser_UserId(sellId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        if (updateBid.getStatus() != BidStatus.OPEN) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "매칭 대기 중인 입찰만 수정할 수 있습니다.");
        }
        updateBid.salePriceUpdate(dto.getPrice());
        // 가격 수정 시에도 매칭 돌려봄
        attemptMatchForSaleBid(updateBid);
    }

    // 구매 입찰 취소
    @Transactional
    public void cancelBuyBid(Long userId, Long buyId) {
        BuyBid cancelBid = buyBidRepository.findByBuyIdAndUser_UserId(buyId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        if (cancelBid.getStatus() != BidStatus.OPEN) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "매칭 대기 중인 입찰만 취소할 수 있습니다.");
        }
        cancelBid.statusUpdate(BidStatus.CANCELED);
    }

    // 판매 입찰 취소
    @Transactional
    public void cancelSaleBid(Long userId, Long sellId) {
        SaleBid cancelBid = saleBidRepository.findBySellIdAndUser_UserId(sellId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        if (cancelBid.getStatus() != BidStatus.OPEN) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "매칭 대기 중인 입찰만 취소할 수 있습니다.");
        }
        cancelBid.statusUpdate(BidStatus.CANCELED);

        // 보관 상품 입찰 취소 시 StorageItem 상태를 ON_SALE → STORED로 복구
        // LAZY 로딩 문제를 피하기 위해 Repository에서 직접 조회
        storageItemRepository.findBySellBidId(sellId).ifPresent(storageItem -> {
//            storageItem.updateStatus(StorageStatus.STORED);
//            log.info("판매 입찰 취소: SaleBidId={}, StorageItem STORED 복구 완료", sellId);
            if (storageItem.getStatus() == StorageStatus.ON_SALE) {
                storageItem.updateStatus(StorageStatus.STORED);
                log.info("판매 입찰 취소: SaleBidId={}, StorageItem 상태를 ON_SALE에서 STORED로 복구 완료", sellId);
            } else {
                log.warn("판매 입찰 취소 시 StorageItem 상태 복구 스킵: SaleBidId={}, 현재 상태={}", sellId, storageItem.getStatus());
            }
        });
    }

    // 구매 입찰 기준 매칭 메소드
    @Transactional
    public void attemptMatchForBuyBid(BuyBid buyBid) {
        // 이미 처리된 입찰 거름
        if (buyBid.getStatus() != BidStatus.OPEN) return;

        // 매칭 대상 saleBids 선점 매칭 대상 없으면 그냥 입찰 목록에 올려둠.
        SaleBid target = saleBidRepository.findMatchTargetForBuy(
                buyBid.getProduct(),
                BidStatus.OPEN,
                buyBid.getPrice(),
                buyBid.getUser().getUserId(),
                PageRequest.of(0, 1)
        ).stream().findFirst().orElse(null);

        if (target == null) return;
        // 선점한 이후에도 여전히 OPEN 상태인지 재확인하여 동시성 문제 방어 추가
        if (buyBid.getStatus() != BidStatus.OPEN || target.getStatus() != BidStatus.OPEN) {
            return;
        }

        // 구매자 빌링키 검증 (자동결제 주체)
        paymentService.validateBillingKey(buyBid.getUser().getUserId());

        // 매칭 대상을 찾았으면 상태 변경
        buyBid.statusUpdate(BidStatus.MATCHED);
        target.statusUpdate(BidStatus.MATCHED);

        // 체결 가격은 sell 가격 (왜? -> 가격 필터가 buy 가격보다 작거나 같게 해놨어서 입찰 올린 가격 보다 더 쌀 수도 있으니까)
        int tradePrice = target.getPrice();
        log.info("구매 입찰 거래가 {} 원으로 체결됨",tradePrice);

        // 주문 생성
        Order order = orderService.createOrder(
                buyBid.getUser(),
                target.getUser(),
                buyBid.getProduct(),
                buyBid,
                target,
                tradePrice,
                BidType.BUY, //구매 입찰이 들어와서 체결됨
                buyBid.getAddressId()
        );

        // 보관 상품 입찰 매칭 시 StorageItem → SOLD 전환
        // LAZY 로딩 문제를 피하기 위해 Repository에서 직접 조회 (getStorageItem() 사용 시 null 반환 버그 있음)
        storageItemRepository.findBySellBidId(target.getSellId()).ifPresent(storageItem -> {
            storageItem.updateStatus(StorageStatus.SOLD);
            log.info("구매 입찰 매칭: SaleBidId={}, StorageItem SOLD 처리 완료", target.getSellId());
        });

        // 저장된 빌링키로 자동 결제 실행
        AutoPaymentRequestDto autoPaymentRequestDto = AutoPaymentRequestDto.builder()
                .userId(buyBid.getUser().getUserId())
                .orderId(order.getOrderId())
                .tossOrderId(paymentService.generateUUID())
                .orderName(buyBid.getProduct().getProductName())
                .build();
        try {
            paymentService.payWithBillingKey(autoPaymentRequestDto);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

    }

    // 판매 입찰 기준 매칭 메소드
    @Transactional
    public void attemptMatchForSaleBid(SaleBid saleBid) {
        // 이미 처리된 입찰 거름
        if (saleBid.getStatus() != BidStatus.OPEN) return;

        // 매칭 대상 buyBids 선점 매칭 대상 없으면 그냥 입찰 목록에 올려둠.
        BuyBid target = buyBidRepository.findMatchTargetForSale(
                saleBid.getProduct(),
                BidStatus.OPEN,
                saleBid.getPrice(),
                saleBid.getUser().getUserId(),
                PageRequest.of(0, 1)
        ).stream().findFirst().orElse(null);

        if (target == null) return;

        // 선점한 이후에도 여전히 OPEN 상태인지 재확인하여 동시성 문제 방어 추가
        if (saleBid.getStatus() != BidStatus.OPEN || target.getStatus() != BidStatus.OPEN) {
            return;
        }

        // 구매자 빌링키 검증
        paymentService.validateBillingKey(target.getUser().getUserId());

        // 매칭 대상을 찾았으면 상태 변경
        saleBid.statusUpdate(BidStatus.MATCHED);
        target.statusUpdate(BidStatus.MATCHED);

        // 체결 가격은 sell 가격
        int tradePrice =  saleBid.getPrice();
        log.info("판매 입찰 거래가 {} 원으로 체결됨",tradePrice);
        // 주문 생성
        Order order = orderService.createOrder(
                target.getUser(),        // buyer (BuyBid 주인)
                saleBid.getUser(),       // seller
                saleBid.getProduct(),
                target,                  // BuyBid
                saleBid,                 // SaleBid
                tradePrice,
                BidType.SELL,
                target.getAddressId()    // 구매자 주소
        );

        // 보관 상품 입찰 매칭 시 StorageItem → SOLD 전환
        // LAZY 로딩 문제를 피하기 위해 Repository에서 직접 조회 (getStorageItem() 사용 시 null 반환 버그 있음)
        storageItemRepository.findBySellBidId(saleBid.getSellId()).ifPresent(storageItem -> {
            storageItem.updateStatus(StorageStatus.SOLD);
            log.info("판매 입찰 매칭: SaleBidId={}, StorageItem SOLD 처리 완료", saleBid.getSellId());
        });

        // 4. 자동결제 실행
        AutoPaymentRequestDto autoPaymentRequestDto = AutoPaymentRequestDto.builder()
                .userId(target.getUser().getUserId())
                .orderId(order.getOrderId())
                .amount(tradePrice)
                .tossOrderId(paymentService.generateUUID())
                .orderName(saleBid.getProduct().getProductName())
                .build();
        try {
            paymentService.payWithBillingKey(autoPaymentRequestDto);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }
}
