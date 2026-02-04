package com.project.rare_x_back.service;

import com.project.rare_x_back.config.TossPaymentClient;
import com.project.rare_x_back.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentCancelService {

    private final PaymentCancelTxService txService;
    private final TossPaymentClient tossPaymentClient;

    public void cancelOnce(Long orderId, long cancelAmount, String reason, String requestedBy) {

        // 락 + 검증 + REQUESTED
        Payment lockedPayment = txService.markRequested(orderId, cancelAmount);

        // 토스 취소 호출
        try {
            tossPaymentClient.cancelPayment(
                    lockedPayment.getTossPaymentKey(),
                    cancelAmount,
                    reason
            );
        } catch (Exception e) {
            // 실패 기록
            txService.markFailed(lockedPayment.getTossPaymentKey());
            throw e;
        }

        // 성공 확정 + 로그 저장
        txService.applySuccess(
                lockedPayment.getTossPaymentKey(),
                cancelAmount,
                reason,
                requestedBy
        );
    }
}