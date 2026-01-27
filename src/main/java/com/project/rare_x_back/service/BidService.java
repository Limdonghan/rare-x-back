package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.PaymentConfirmRequestDto;
import com.project.rare_x_back.dto.request.PurchaseRequestDto;
import com.project.rare_x_back.dto.request.RegisterBuyBidRequestDto;
import com.project.rare_x_back.dto.request.RegisterSaleBidRequestDto;
import com.project.rare_x_back.dto.response.OrderShipResponseDto;
import com.project.rare_x_back.dto.response.PurchaseResponseDto;
import com.project.rare_x_back.dto.response.RegisterBuyBidResponseDto;
import com.project.rare_x_back.dto.response.RegisterSaleBidResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.*;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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

    private final OrderService orderService;

    /**
     * [판매 입찰 등록]
     * 1. 판매자가 상품을 등록 (완료)
     * 2. 구매자가 있으면 즉시체결 OR 자동결제 -> 매칭이되면.?
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
     * [즉시 결제 처리]
     * 1. 구매자가 해당 상품 선택
     * 2. 주문 테이블 생성
     * 3. 입찰 상태 변경
     * 4. 결제 시도
     * */
    @Transactional
    public PurchaseResponseDto purchaseNow(PurchaseRequestDto purchaseRequestDto, String email){
        User buyer = userRepository.findByEmailAndIsDeletedFalse(email)
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

        /// 결제 승인
        PaymentConfirmRequestDto paymentConfirmRequestDto = PaymentConfirmRequestDto.builder()
                .paymentKey(purchaseRequestDto.getPaymentKey())
                .tossOrderId(purchaseRequestDto.getTossOrderId())
                .amount(purchaseRequestDto.getAmount())
                .orderId(order)
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

    // 구매 입찰 기준 매칭 메소드
    @Transactional
    public void  attemptMatchForBuyBid(BuyBid buyBid) {
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

        // 매칭 대상을 찾았으면 상태 변경
        buyBid.statusUpdate(BidStatus.MATCHED);
        target.statusUpdate(BidStatus.MATCHED);

        // 체결 가격은 sell 가격 (왜? -> 가격 필터가 buy 가격보다 작거나 같게 해놨어서 입찰 올린 가격 보다 더 쌀 수도 있으니까)
        int tradePrice = target.getPrice();

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
        // 이건 페이먼츠 되면 ...
        // paymentService.payWithBillingKey(buyBid.getUser(), order, tradePrice);
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
                PageRequest.of(0,1)
        ).stream().findFirst().orElse(null);

        if (target == null) return;

        // 선점한 이후에도 여전히 OPEN 상태인지 재확인하여 동시성 문제 방어 추가
        if (saleBid.getStatus() != BidStatus.OPEN || target.getStatus() != BidStatus.OPEN) {
            return;
        }

        // 매칭 대상을 찾았으면 상태 변경
        saleBid.statusUpdate(BidStatus.MATCHED);
        target.statusUpdate(BidStatus.MATCHED);

        // 체결 가격은 buy 가격 (왜? -> 가격 필터가 sale 가격보다 크거나 같게 해놨어서 입찰 올린 가격 보다 더 쌀 수도 있으니까)
        int tradePrice = target.getPrice();

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
        // 이건 페이먼츠 되면 ...
        // paymentService.payWithBillingKey(buyBid.getUser(), order, tradePrice);

    }

}
