package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.PreOccupancyLockRequestDto;
import com.project.rare_x_back.dto.request.PurchaseRequestDto;
import com.project.rare_x_back.dto.request.RegisterBuyBidRequestDto;
import com.project.rare_x_back.dto.request.RegisterSaleBidRequestDto;
import com.project.rare_x_back.dto.request.SellNowRequestDto;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bid")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    /// 수수료 설정 조회
    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<FeeResponseDto>> getFees() {
        FeeResponseDto feeResponseDto = bidService.getFees();
        return ResponseEntity.ok(ApiResponse.success(feeResponseDto, "수수료 정보 조회가 완료되었습니다."));
    }

    /// 판매 입찰 등록
    @PostMapping("/sale")
    public ResponseEntity<ApiResponse<RegisterSaleBidResponseDto>> registerSaleBid(@Valid @RequestBody RegisterSaleBidRequestDto registerSaleBidRequestDto,
                                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        RegisterSaleBidResponseDto registerSaleBidResponseDto = bidService.registerSaleBid(registerSaleBidRequestDto, userDetails.getUsername());

        return ResponseEntity
                .ok().body(ApiResponse.success(registerSaleBidResponseDto,"판매 신청이 완료되었습니다."));
    }

    /// 구매 입찰 등록
    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<RegisterBuyBidResponseDto>> registerBuyBid(@Valid @RequestBody RegisterBuyBidRequestDto registerBuyBidRequestDto,
                                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        RegisterBuyBidResponseDto registerBuyBidResponseDto = bidService.registerBuyBid(registerBuyBidRequestDto, userDetails.getUsername());

        return ResponseEntity
                .ok().body(ApiResponse.success(registerBuyBidResponseDto,"구매 신청이 완료되었습니다."));
    }


    /**
     * [즉시 구매] (Buy Now)
     * 일반 결제(Toss Window) 후 호출되는 API
     * 결제 승인 + 거래 체결(Order 생성)을 한 번에 처리
     */
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> purchaseNow(@Valid @RequestBody PurchaseRequestDto purchaseRequestDto,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        PurchaseResponseDto purchaseResponseDto = bidService.purchaseNow(purchaseRequestDto, userDetails.getUsername());
        return ResponseEntity
                .ok().body(ApiResponse.success(purchaseResponseDto,"즉시 구매가 완료되었습니다"));
    }

    /**
     * [즉시 판매] (Sell Now)
     * 판매자가 '판매하기' 버튼을 누르면, 가장 비싸게 부른 구매자와 즉시 체결
     * -> 구매자의 카드를 '자동 결제' 처리
     */
    @PostMapping("/sell")
    public ResponseEntity<ApiResponse<SellNowResponseDto>> sellNow(@Valid @RequestBody SellNowRequestDto sellNowRequestDto,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        SellNowResponseDto sellNowResponseDto = bidService.sellNow(sellNowRequestDto, userDetails.getUsername());
        return ResponseEntity
                .ok().body(ApiResponse.success(sellNowResponseDto,"즉시 판매가 완료되었습니다"));
    }

    /**
     * [즉시 구매 선점 락 발급] (Lock)
     * 결제 위젯 진입 전, 가장 저렴한 SaleBid에 임시로 5~10분간 락을 걸어서 타 사용자의 접근을 차단
     */
    @PostMapping("/purchase/lock")
    public ResponseEntity<ApiResponse<PreOccupancyLockResponseDto>> acquirePurchaseLock(
            @Valid @RequestBody PreOccupancyLockRequestDto lockRequestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        PreOccupancyLockResponseDto lockedDto = bidService.acquirePurchaseLock(lockRequestDto, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(lockedDto, "결제 대행 락을 성공적으로 획득했습니다."));
    }

    /**
     * [즉시 구매 선점 락 해제] (Unlock)
     * 결제 취소창을 닫거나 에러 난 경우 브라우저가 호출하여 임시 락 즉시 해제
     */
    @DeleteMapping("/purchase/lock/{sellId}")
    public ResponseEntity<ApiResponse<Void>> releasePurchaseLock(
            @PathVariable Long sellId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
            
        bidService.releasePurchaseLock(sellId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "결제 대행 락이 해제되었습니다."));
    }

}
