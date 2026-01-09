package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.BillingKeyRequestDto;
import com.project.rare_x_back.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    /**
     * [카드 등록 API]
     * 프론트엔드에서 tossPayments.requestBillingAuth() 성공 후 받은
     * authKey와 customerKey를 넘겨주면 됩니다.
     */

    @PostMapping("/billing/register")
    public ApiResponse<?> registerCard (@RequestBody BillingKeyRequestDto requestDto,
                                        @RequestParam String email) {

        //paymentService.registerCard(userId, authKey, customerKey,email);

        return ApiResponse.success(paymentService.registerCard(requestDto,email));
    }
}
