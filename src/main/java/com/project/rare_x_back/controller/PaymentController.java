package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.AutoPaymentRequestDto;
import com.project.rare_x_back.dto.request.BillingKeyRequestDto;
import com.project.rare_x_back.dto.request.PaymentConfirmRequestDto;
import com.project.rare_x_back.dto.response.BillingKeyResponseDto;
import com.project.rare_x_back.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    /**
     * [카드 등록 API]
     * 프론트엔드에서 tossPayments.requestBillingAuth() 성공 후 받은
     * authKey와 customerKey를 전달
     */
    @PostMapping("/billing/register")
    public ApiResponse<?> registerCard (@RequestBody BillingKeyRequestDto requestDto,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(paymentService.registerCard(requestDto, userDetails.getUsername()));
    }

    /**
     * [카드 삭제 API]
     * 해당 유저를 검증 후 유저가 등록한 빌링키를 찾아 삭제
     */
    @DeleteMapping("/billing")
    public ApiResponse<?> deleteBillingKey (@AuthenticationPrincipal CustomUserDetails userDetails) {
        paymentService.deleteBillingKey(userDetails.getUsername());
        return ApiResponse.success("빌링키 삭제 완료");
    }

    /**
     * [자동 결제 요청 API]
     * 등록된 카드로 즉시 결제를 진행
     * 요청 예시: POST /api/payments/billing/pay?email=buyer1@test.com
     */
    @PostMapping("/billing/pay")
    public ApiResponse<?> payWithBillingKey(@RequestBody AutoPaymentRequestDto requestDto) {
        return ApiResponse.success(paymentService.payWithBillingKey(requestDto));
    }


    /**
     * [일반 결제 승인 API]
     * 요청 예시: POST /api/payments/confirm?email=buyer1@test.com
     */
    @PostMapping("/confirm")
    public ApiResponse<?> confirmPayment(@RequestBody PaymentConfirmRequestDto requestDto,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(paymentService.confirmPayment(requestDto,userDetails.getUsername()));
    }

    /**
     * [Toss UUID 발급용 API]
     * 프론트엔드에서 결제 위젯을 띄우기 전 호출하여 orderId를 받아감
     */
    @GetMapping("/created-uuid")
    public ApiResponse<String> generateUUID() {
        String uuid = paymentService.generateUUID();
        return ApiResponse.success(uuid, "UUID 생성 완료");
    }

    /**
     * [Toss 빌링키 존재 체크]
     */
    @GetMapping("/billing-key-check")
    public ApiResponse<BillingKeyResponseDto> checkBillingKey(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        BillingKeyResponseDto billingKeyResponseDto = paymentService.validateBillingKey(userDetails.getUserId());

        return ApiResponse.success(billingKeyResponseDto,"Toss 빌링키 체크");
    }

}
