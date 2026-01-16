package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.AutoPaymentRequestDto;
import com.project.rare_x_back.dto.request.BillingKeyRequestDto;
import com.project.rare_x_back.dto.request.PaymentConfirmRequestDto;
import com.project.rare_x_back.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                                        @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(paymentService.registerCard(requestDto, userId));
    }

    /**
     * [자동 결제 요청 API]
     * 등록된 카드로 즉시 결제를 진행
     * 요청 예시: POST /api/payments/billing/pay?email=buyer1@test.com
     */
    @PostMapping("/billing/pay")
    public ApiResponse<?> payWithBillingKey(@RequestBody AutoPaymentRequestDto requestDto,
                                            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(paymentService.payWithBillingKey(requestDto,userId));
    }


    /**
     * [일반 결제 승인 API]
     * 요청 예시: POST /api/payments/confirm?email=buyer1@test.com
     */
    @PostMapping("/confirm")
    public ApiResponse<?> confirmPayment(@RequestBody PaymentConfirmRequestDto requestDto,
                                         @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(paymentService.confirmPayment(requestDto,userId));
    }
}
