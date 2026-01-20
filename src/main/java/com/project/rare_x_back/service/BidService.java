package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.PaymentConfirmRequestDto;
import com.project.rare_x_back.dto.request.PurchaseRequestDto;
import com.project.rare_x_back.dto.request.RegisterBuyBidRequestDto;
import com.project.rare_x_back.dto.request.RegisterSaleBidRequestDto;
import com.project.rare_x_back.dto.response.PurchaseResponseDto;
import com.project.rare_x_back.dto.response.RegisterBuyBidResponseDto;
import com.project.rare_x_back.dto.response.RegisterSaleBidResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.enums.BidType;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * [판매 입찰 등록]
     * 1. 판매자가 상품을 등록 (완료)
     * 2. 구매자가 있으면 즉시체결 OR 자동결제
     * */
    @Transactional
    public RegisterSaleBidResponseDto registerSaleBid(RegisterSaleBidRequestDto registerSaleBidRequestDto, String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("user not found"));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(registerSaleBidRequestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("product not found"));


        /// 보관 판매 확인. 보관 판매가 아닐경우 DB에 NULL로 저장 및 Response에 false 출력
        StorageItem storageItem = storageItemRepository.findByProduct(product);
        boolean storageItemCheck=false;
        if (storageItem.getStorageId() != null){
            storageItem = storageItemRepository.findByProduct(product);
            storageItemCheck=true;
        }

        SaleBid build = SaleBid.builder()
                .userId(user)
                .productId(product)
                .price(registerSaleBidRequestDto.getPrice())
                .status(BidStatus.OPEN)
                .storageId(storageItem)
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
                .orElseThrow(() -> new RuntimeException("user not found"));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(registerBuyBidRequestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("product not found"));

        BuyBid build = BuyBid.builder()
                .userId(user)
                .productId(product)
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
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("user not found"));
        Product product = productRepository.findByProductIdAndIsDeletedFalse(purchaseRequestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("product not found"));

        /// [매칭] 해당 가격에 파는 판매 입찰(SaleBid) 찾기, (가장 저렴하고, 먼저 등록된 판매 입찰 1개 조회)
        List<SaleBid> saleBidList = saleBidRepository.findAllByProductIdAndPriceAndStatusOrderByCreatedAtAsc(product, purchaseRequestDto.getPrice(), BidStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("판매중인 상품이 없습니다."));
        SaleBid saleBid = saleBidList.getFirst();

        /// [상태 변경] 판매 입찰 -> 체결됨(MATCHED)
        saleBid.statusUpdate(BidStatus.MATCHED);

        /// [주문 생성] Order 만들기
        Order build = Order.builder()
                .buyer(user)
                .seller(saleBid.getUserId())
                .productId(product)
                .buyBidId(null)
                .sellBidId(saleBid)
                .type(BidType.BUY)
                .price(purchaseRequestDto.getPrice())
                .currentStatus(CurrentStatus.PENDING_INSPECTION)
                .build();
        orderRepository.save(build);

        /// 결제 승인
        PaymentConfirmRequestDto paymentConfirmRequestDto = PaymentConfirmRequestDto.builder()
                .paymentKey(purchaseRequestDto.getPaymentKey())
                .tossOrderId(purchaseRequestDto.getTossOrderId())
                .amount(purchaseRequestDto.getAmount())
                .orderId(build.getOrderId())
                .build();
        paymentService.confirmPayment(paymentConfirmRequestDto,email);

        return PurchaseResponseDto.builder()
                .productName(product.getProductName())
                .brandName(product.getBrand().getBrandName())
                .category(product.getCategory().getCategoryName())
                .tossOrderId(purchaseRequestDto.getTossOrderId())
                .amount(purchaseRequestDto.getAmount())
                .amount(purchaseRequestDto.getAmount())
                .build();



    }


}
