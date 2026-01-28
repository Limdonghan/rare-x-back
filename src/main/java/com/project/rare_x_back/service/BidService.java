package com.project.rare_x_back.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final StorageItemRepository storageItemRepository;
    private final InspectionRepository inspectionRepository;
    @Value("${inspection-center.address}")
    private String inspectionCenterAddress;
    @Value("${inspection-center.zipcode}")
    private String inspectionCenterZipcode;

    /**
     * [판매 입찰 등록]
     * 1. 판매자가 상품을 등록 (완료)
     * 2. 구매자가 있으면 즉시체결 OR 자동결제
     * */
    @Transactional
    public RegisterSaleBidResponseDto registerSaleBid(RegisterSaleBidRequestDto registerSaleBidRequestDto, String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(registerSaleBidRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));


        /// 보관 판매 확인. 보관 판매가 아닐경우 DB에 NULL로 저장 및 Response에 false 출력
        StorageItem storageItem = storageItemRepository.findByProduct(product);
        boolean storageItemCheck=false;
        if (storageItem!=null && storageItem.getStorageId() != null){
            storageItem = storageItemRepository.findByProduct(product);
            storageItemCheck=true;
        }

        SaleBid build = SaleBid.builder()
                .user(user)
                .product(product)
                .price(registerSaleBidRequestDto.getPrice())
                .status(BidStatus.OPEN)
                .storageItem(storageItem)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        saleBidRepository.save(build);

        return RegisterSaleBidResponseDto.builder()
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
     * */
    @Transactional
    public RegisterBuyBidResponseDto registerBuyBid(RegisterBuyBidRequestDto registerBuyBidRequestDto, String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(registerBuyBidRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));


        BuyBid build = BuyBid.builder()
                .user(user)
                .product(product)
                .price(registerBuyBidRequestDto.getPrice())
                .status(BidStatus.OPEN)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        buyBidRepository.save(build);
        return RegisterBuyBidResponseDto.builder()
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
     * */
    @Transactional
    public PurchaseResponseDto purchaseNow(PurchaseRequestDto purchaseRequestDto, String email){
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(purchaseRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        /// [매칭] 해당 가격에 파는 판매 입찰(SaleBid) 찾기, (가장 저렴하고, 먼저 등록된 판매 입찰 1개 조회)
        List<SaleBid> saleBidList = saleBidRepository
                .findAllByProductAndPriceAndStatusOrderByCreatedAtAsc(product, purchaseRequestDto.getPrice(), BidStatus.OPEN);
        if (saleBidList.isEmpty()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        SaleBid saleBid = saleBidList.getFirst();

        /// [상태 변경] 판매 입찰 -> 체결됨(MATCHED)
        saleBid.statusUpdate(BidStatus.MATCHED);

        /// [주문 생성] Order 만들기
        Order order = Order.builder()
                .buyer(user)
                .seller(saleBid.getUser())
                .product(product)
                .buyBid(null)
                .sellBid(saleBid)
                .type(BidType.BUY)
                .price(purchaseRequestDto.getPrice())
                .currentStatus(CurrentStatus.PENDING)
                .sellerShippedAt(LocalDateTime.now().plusDays(2))
                .build();
        Order saveOrder = orderRepository.save(order);

        /// 결제 승인
        PaymentConfirmRequestDto paymentConfirmRequestDto = PaymentConfirmRequestDto.builder()
                .paymentKey(purchaseRequestDto.getPaymentKey())
                .tossOrderId(purchaseRequestDto.getTossOrderId())
                .amount(purchaseRequestDto.getAmount())
                .orderId(saveOrder.getOrderId())
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

        // 5. Order 상태 변경
        order.setCurrentStatus(CurrentStatus.SHIPPED_TO_WAREHOUSE);

        // 6. Inspection 생성
        Inspection inspection = Inspection.builder()
                .order(order)
                .type(InspectionType.ORDER)
                .status(InspectionStatus.SHIPPED_TO_WAREHOUSE)
                .build();
        inspectionRepository.save(inspection);

        return OrderShipResponseDto.from(order, inspectionCenterAddress, inspectionCenterZipcode);
    }

    /**
     * [즉시 판매 결제 처리]
     * 1. 구매자가 해당 상품 선택
     * 2. 주문 테이블 생성
     * 3. 입찰 상태 변경
     * 4. 결제 시도
     * */
    @Transactional
    public SellNowResponseDto sellNow (SellNowRequestDto sellNowRequestDto, String email) {

        ///  판매자 조회
        User seller = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(sellNowRequestDto.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        BuyBid buyBid = buyBidRepository.findById(sellNowRequestDto.getBidId()).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_ON_BID));

        String orderNumber = paymentService.createTossOrderId();

        /// [상태 변경] 구매입찰 -> 체결됨
        buyBid.statusUpdate(BidStatus.MATCHED);

        Order build = Order.builder()
                .buyer(buyBid.getUser())
                .seller(seller)
                .product(product)
                .buyBid(buyBid)
                .sellBid(null)
                .type(BidType.SELL)
                .price(sellNowRequestDto.getPrice())
                .currentStatus(CurrentStatus.PENDING)
                .shipDeadline(LocalDateTime.now().plusDays(2))
                .build();
        Order saveOrder = orderRepository.save(build);

        AutoPaymentRequestDto autoPaymentRequestDto = AutoPaymentRequestDto.builder()
                .userId(buyBid.getUser().getUserId())
                .orderId(saveOrder.getOrderId())
                .amount(saveOrder.getPrice())
                .tossOrderId(orderNumber)
                .orderName(product.getProductName())
                .build();

        try {
            paymentService.payWithBillingKey(autoPaymentRequestDto);
        }catch (Exception e) {
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

}
