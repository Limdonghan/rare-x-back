package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.Payment;
import com.project.rare_x_back.entity.PaymentCancel;
import com.project.rare_x_back.enums.CancelStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.PaymentCancelRepository;
import com.project.rare_x_back.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelTxService {

    // 결제 취소 트랜잭션 전용 서비스
    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;

     // 취소 요청 락 + 검증 + REQUESTED
    @Transactional
    public Payment markRequested(Long orderId, int cancelAmount) {
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        //  락 걸고 조회
        Payment locked = paymentRepository.findByTossPaymentKeyForUpdate(payment.getTossPaymentKey())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!locked.getStatus().equals("DONE")) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "결제 완료 상태가 아니라 취소할 수 없습니다.");
        }

        if (locked.getCancelStatus() != CancelStatus.NONE) {
            throw new CustomException(ErrorCode.ALREADY_CANCELED);
        }

        if (cancelAmount <= 0 || cancelAmount > locked.getAmount()) {
            throw new CustomException(ErrorCode.INVALID_CANCEL_AMOUNT);
        }

        locked.markCancelRequested();
        return locked;
    }


     // 취소 성공 확정 락 + 상태 확정 + 취소 로그 저장
    @Transactional
    public void applySuccess(String paymentKey, int cancelAmount, String reason, String requestedBy) {
        //  락 걸고 조회
        Payment locked = paymentRepository.findByTossPaymentKeyForUpdate(paymentKey)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 취소 로그 있으면(멱등) 그대로 종료
        if (paymentCancelRepository.existsByPayment_PaymentId(locked.getPaymentId())) {
            return;
        }

        locked.applyCancelSuccess(cancelAmount);

        try {
            paymentCancelRepository.save(
                    PaymentCancel.builder()
                            .payment(locked)
                            .cancelAmount(cancelAmount)
                            .cancelReason(reason)
                            .requestedBy(requestedBy)
                            .canceledAt(LocalDateTime.now())
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // payment_id UNIQUE 위반이면 이미 저장된 것 → 멱등 성공 처리
            log.warn("중복 취소 로그 감지(payment_id UNIQUE). paymentId={}", locked.getPaymentId());
        }
    }


     // 취소 실패 기록: 락 + CANCEL_FAILED
    @Transactional
    public void markFailed(String paymentKey) {
        Payment locked = paymentRepository.findByTossPaymentKeyForUpdate(paymentKey)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        locked.markCancelFailed();
    }
}
