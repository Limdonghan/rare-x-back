package com.project.rare_x_back.config;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentClient {

    /**
     * 토스 결제 API 전용 WebClient
     * - baseUrl / Authorization / Toss-Version 설정됨
     */
    private final WebClient webClient;

    /**
     * 결제 취소 요청 (전액 / 부분 공용)
     * @param paymentKey  토스 결제 키
     * @param cancelAmount 환불 금액
     * @param reason 취소 사유 (로그용)
     */
    public void cancelPayment(
            String paymentKey,
            long cancelAmount,
            String reason
    ) {

        // 취소 요청 Body
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", reason);

        // 토스 정책 상 cancelAmount가 있으면 부분 취소,없으면 전액 취소
        // 우리는 항상 금액 명시 -> 전액/부분 분기 헷갈림 방지

        body.put("cancelAmount", cancelAmount);

        try {
            webClient.post()
                    // POST /payments/{paymentKey}/cancel
                    .uri("/payments/{paymentKey}/cancel", paymentKey)
                    .bodyValue(body)
                    .retrieve()

                    // 4xx → 우리가 잘못 요청
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class)
                                    .map(msg -> {
                                        log.error(
                                                "토스 결제 취소 4xx 오류 paymentKey={}, msg={}",
                                                paymentKey, msg
                                        );
                                        return new CustomException(
                                                ErrorCode.PAYMENT_FAILED,
                                                "결제 취소 요청 오류"
                                        );
                                    })
                    )

                    // 5xx → 토스 서버 문제
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.bodyToMono(String.class)
                                    .map(msg -> {
                                        log.error(
                                                "토스 결제 취소 5xx 오류 paymentKey={}, msg={}",
                                                paymentKey, msg
                                        );
                                        return new CustomException(
                                                ErrorCode.TOSS_API_ERROR,
                                                "토스 서버 오류"
                                        );
                                    })
                    )

                    // 응답 Body는 사용 안 함
                    .bodyToMono(Void.class)
                    .block();

        } catch (CustomException e) {
            // 이미 의미 있는 예외 → 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 네트워크 / 타임아웃 / 알 수 없는 오류
            log.error(
                    "토스 결제 취소 예외 paymentKey={}, cancelAmount={}",
                    paymentKey, cancelAmount, e
            );
            throw new CustomException(
                    ErrorCode.TOSS_API_ERROR,
                    "결제 취소 중 예외 발생"
            );
        }
    }
}
