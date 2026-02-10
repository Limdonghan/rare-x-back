package com.project.rare_x_back.service;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.dto.request.AutoPaymentRequestDto;
import com.project.rare_x_back.dto.request.BillingKeyRequestDto;
import com.project.rare_x_back.dto.request.PaymentConfirmRequestDto;
import com.project.rare_x_back.dto.response.BillingKeyResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.PaymentHistoryStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final WebClient webClient;
    private final PaymentRepository paymentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final SettlementService settlementService;
    private final PaymentCancelTxService txService;

    /**
     * 카드 등록 (빌링키 발급)
     * 프론트에서 받은 authKey로 실제 결제 가능한 billingKey를 받아와 저장
     */
    @Transactional
    public BillingKey registerCard(BillingKeyRequestDto billingKeyRequestDto, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        /// 3. Billing_Key DB에 기존에 등록한 유저가 있다면 삭제 후 갱신 (또는 추가)
        billingKeyRepository.findByUser(user).ifPresent(billingKey -> {
                    log.info("기존 빌링키 삭제: User {}", user.getUserId());
                    billingKeyRepository.delete(billingKey);
                }
        );


        try {
            /// 1. 토스에 빌링키 발급 요청
            Map<String, Object> response = webClient.post()                     /// POST 요청
                    .uri("billing/authorizations/issue")    /// 엔드포인트 설정
                    .bodyValue(Map.of(
                            "authKey", billingKeyRequestDto.getAuthKey(),
                            "customerKey", billingKeyRequestDto.getCustomerKey()
                    ))                                          /// Request Body 설정
                    .retrieve()                                 /// 실제 HTTP 요청 실행
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.PAYMENT_FAILED,"결제 정보 오류" + s)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.TOSS_API_ERROR,"토스 서버 오류" + s)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})                     /// 응답을 Map으로 반환
                    .block();                                   /// 동기식으로 대기

            // TODO: Order 조회 및 검증 로직 추가 (DB의 주문 금액과 일치하는지 등)

            /// 2. 응답에서 필요한 정보 추출
            String billingKey = (String) response.get("billingKey");
            Map<String,Object> cardInfo = (Map<String, Object>) response.get("card");

            /// Response값 매핑
            String customerKey = String.valueOf(response.get("customerKey"));
            String cardCompany = String.valueOf(response.get("cardCompany"));
            String cardNumber = String.valueOf(cardInfo.get("number")).substring(12,16);
            OffsetDateTime authenticatedAt = OffsetDateTime.parse((String) response.get("authenticatedAt"));

            /// 4. DB 저장
            BillingKey build = BillingKey.builder()
                    .user(user)
                    .customerKey(customerKey)
                    .billingKey(billingKey)
                    .cardCompany(cardCompany)
                    .cardNumber(cardNumber)
                    .authenticatedAt(authenticatedAt)
                    .build();

            return billingKeyRepository.save(build);

        } catch (Exception e) {
            log.error("결제 승인 실패: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 자동 결제 (빌링키 사용)
     * 저장된 빌링키를 사용하여 비밀번호 없이 즉시 결제 승인 요청
     * */
    @Transactional
    public Payment payWithBillingKey (AutoPaymentRequestDto autoPaymentRequestDto) {

        /// [중복 검사] 이미 저장된 결제인지 확인
        Payment existingPayment = paymentRepository.findByTossPaymentKey(autoPaymentRequestDto.getPaymentKey()).orElse(null);
        if (existingPayment != null) {
            /// 동일 paymentKey에 대해 요청 파라미터가 일치하는지 검증 (idempotent 요청 처리)
            boolean matchesAmount = existingPayment.getAmount()==autoPaymentRequestDto.getAmount();
            boolean matchesOrderId = existingPayment.getTossOrderId().equals(autoPaymentRequestDto.getOrderId());

            if (matchesAmount && matchesOrderId) {
                return existingPayment;
            } else {
                log.error("기존 paymentKey에 대한 결제 파라미터 불일치: paymentKey={}, existingAmount={}, requestAmount={}, existingOrderId={}, requestOrderId={}",
                        autoPaymentRequestDto.getPaymentKey(),
                        existingPayment.getAmount(), autoPaymentRequestDto.getAmount(),
                        existingPayment.getTossOrderId(), autoPaymentRequestDto.getOrderId());
                throw new RuntimeException("기존 paymentKey에 대한 결제 정보가 요청과 일치하지 않습니다.");
            }
        }

        /// 1. 유저 확인
        User user = userRepository.findById(autoPaymentRequestDto.getUserId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(autoPaymentRequestDto.getOrderId()).orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        /// 2. DB에서 저장된 빌링키 꺼내오기
        BillingKey billingKey = billingKeyRepository.findByUser(user).orElseThrow(() -> new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND));

        // 수수료 및 배송비 계산한 최종 가격
        int buyerTotalAmount = FeeCalculator.buyerTotalAmount(order.getPrice());

        try {
            /// 3. 토스 API 호출
            Map<String, Object> response = webClient.post()
                    .uri("billing/" + billingKey.getBillingKey())
                    .bodyValue(Map.of(
                            "amount", buyerTotalAmount,
                            "customerKey", billingKey.getCustomerKey(),
                            "orderId", autoPaymentRequestDto.getTossOrderId(),
                            "orderName", autoPaymentRequestDto.getOrderName()
                    ))                                          /// Request Body 설정
                    .retrieve()                                 /// 실제 HTTP 요청 실행
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.PAYMENT_FAILED,"결제 정보 오류" + s)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.TOSS_API_ERROR,"토스 서버 오류" + s)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })                      /// 응답을 Map으로 반환
                    .block();                                   /// 동기식으로 대기
            // 페이먼츠 저장
            Payment payment = responseMappingWithSave(response, order);

            // 페이먼츠 히스토리 생성
            createPaymentHistory(order);
            // 정산 레코드 생성 (PENDING)
            settlementService.createSettlement(order);

            return payment;

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

    }



    /**
     * 즉시 결제
     * 프론트에서 결제창을 통해 인증된 건을 최종 승인(Confirm)
     * */
    @Transactional
    public Payment confirmPayment (PaymentConfirmRequestDto paymentConfirmRequestDto, String userEmail) {

        /// [중복 검사] 이미 저장된 결제인지 확인
        if (paymentRepository.findByTossPaymentKey(paymentConfirmRequestDto.getPaymentKey()).isPresent()) {
            /// 이미 저장되어 있다면 에러를 내지 말고, 저장된 정보를 그대로 반환 (또는 에러 처리)
            return paymentRepository.findByTossPaymentKey(paymentConfirmRequestDto.getPaymentKey()).get();
        }

        /// 1. 유저 검증
        userRepository.findByEmail(userEmail).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(paymentConfirmRequestDto.getOrderId()).orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 수수료 및 배송비 계산한 최종 가격
        int buyerTotalAmount = FeeCalculator.buyerTotalAmount(order.getPrice());

        try {
            /// 2. 토스 API 호출
            Map<String, Object> response = webClient.post()
                    .uri("payments/confirm")
                    .bodyValue(Map.of(
                            "paymentKey", paymentConfirmRequestDto.getPaymentKey(),
                            "orderId", paymentConfirmRequestDto.getTossOrderId(),
                            "amount", buyerTotalAmount
                    ))                                                                                          /// Request Body 설정
                    .retrieve()                                                                                 /// 실제 HTTP 요청 실행
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.PAYMENT_FAILED,"결제 정보 오류" + s)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.TOSS_API_ERROR,"토스 서버 오류" + s)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })                       /// 응답을 Map으로 반환
                    .block();                                                                                   /// 동기식으로 대기

            Payment payment = responseMappingWithSave(response,order);

            // 페이먼츠 히스토리 생성
            createPaymentHistory(order);
            // 정산 레코드 생성 (PENDING)
            settlementService.createSettlement(order);

            return payment;

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 보관료 빌링키 결제
     * Order 없이 빌링키로 결제 후 tossPaymentKey 반환
     *
     * @param userId 사용자 ID
     * @param amount 결제 금액
     * @return tossPaymentKey (결제 취소/조회에 사용)
     */
    public String payStorageFeeWithBillingKey(Long userId, int amount) {

        // 1. 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 빌링키 조회
        BillingKey billingKey = billingKeyRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.BILLING_KEY_NOT_FOUND));

        // 3. 토스용 주문 ID 생성
        String tossOrderId = "STORAGE_" + generateUUID();
        String orderName = "보관료 결제";

        try {
            // 4. 토스 API 호출
            Map<String, Object> response = webClient.post()
                    .uri("billing/" + billingKey.getBillingKey())
                    .bodyValue(Map.of(
                            "amount", amount,
                            "customerKey", billingKey.getCustomerKey(),
                            "orderId", tossOrderId,
                            "orderName", orderName
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.PAYMENT_FAILED, "보관료 결제 실패: " + s)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(s -> new CustomException(ErrorCode.TOSS_API_ERROR, "토스 서버 오류: " + s)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            // 5. paymentKey 추출 및 반환
            String tossPaymentKey = String.valueOf(response.get("paymentKey"));
            log.info("보관료 결제 성공: userId={}, amount={}, paymentKey={}", userId, amount, tossPaymentKey);

            return tossPaymentKey;

        } catch (CustomException e) {
            log.error("보관료 결제 실패: userId={}, amount={}, error={}", userId, amount, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("보관료 결제 중 예외 발생: userId={}, amount={}, error={}", userId, amount, e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_FAILED, "보관료 결제 처리 중 오류 발생");
        }
    }

    /**
     * 공통 메서드 처리
     * */
    private Payment responseMappingWithSave (Map<String, Object> response, Order order){
        Map<String, Object> cardInfo = (Map<String, Object>) response.get("card");

        /// Response 값 매핑
        String tossOrderId = String.valueOf(response.get("orderId"));
        String tossPaymentKey = String.valueOf(response.get("paymentKey"));
        int amount = (Integer) cardInfo.get("amount");
        String method = String.valueOf(response.get("method"));
        String status = String.valueOf(response.get("status"));
        String type = String.valueOf(response.get("type"));
        OffsetDateTime requestedAt = OffsetDateTime.parse((String) response.get("requestedAt"));
        OffsetDateTime approvedAt = OffsetDateTime.parse((String) response.get("approvedAt"));

        /// DB 저장
        Payment build = Payment.builder()
                .order(order)
                .tossOrderId(tossOrderId)
                .tossPaymentKey(tossPaymentKey)
                .amount(amount)
                .method(method)
                .status(status)
                .type(type)
                .requestedAt(requestedAt)
                .approvedAt(approvedAt)
                .build();
        return paymentRepository.save(build);
    }


    public String generateUUID(){
        return UUID.randomUUID().toString();
    }


    // 빌링키 존재 체크
    public BillingKeyResponseDto validateBillingKey(Long userId) {

        boolean exists = billingKeyRepository.existsByUser_UserId(userId);

        BillingKey billingKey = billingKeyRepository.findByUserUserId(userId);

        if (exists) {
            return BillingKeyResponseDto.builder()
                    .cardCompany(billingKey.getCardCompany())
                    .cardNumber(billingKey.getCardNumber())
                    .hasBillingKey(true)
                    .build();
        } else {
            return BillingKeyResponseDto.builder()
                    .cardCompany(null)
                    .cardNumber(null)
                    .hasBillingKey(false)
                    .build();
        }

    }


    // 페이먼츠 히스토리 생성 메소드
    private void createPaymentHistory(Order order) {
        int buyerFee = FeeCalculator.buyerFee(order.getPrice());
        int totalAmount = FeeCalculator.buyerTotalAmount(order.getPrice());

        PaymentHistory history = PaymentHistory.builder()
                .order(order)
                .buyer(order.getBuyer())
                .buyBid(order.getBuyBid())
                .commissionFee(buyerFee)
                .deliveryFee(3000)
                .totalAmount(totalAmount)
                .status(PaymentHistoryStatus.COMPLETE)
                .reqDate(order.getCreatedAt())
                .resDate(LocalDateTime.now())
                .build();
        paymentHistoryRepository.save(history);
    }

    /**
     * 결제 취소 요청 (전액 / 부분 공용)
     * @param paymentKey  토스 결제 키
     * @param idempotencyKey 멱등키
     * @param cancelAmount 환불 금액
     * @param reason 취소 사유 (로그용)
     */
    private void cancelPayment(
            String paymentKey,
            String idempotencyKey,  /// [추가] 멱등키 변수
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
                    .header("Idempotency-Key", idempotencyKey)  /// [추가] 멱등키 헤더 추가
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

    /**
     * [결제 취소 요청 - 단건]
     * DB 트랜잭션과 외부 API 호출을 분리하여 안전하게 결제 취소 진행
     */
    public void cancelOnce(Long orderId, long cancelAmount, String reason, String requestedBy) {

        // 락 + 검증 + REQUESTED
        Payment lockedPayment = txService.markRequested(orderId, cancelAmount);

        // 토스 취소 호출
        try {
            cancelPayment(
                    lockedPayment.getTossPaymentKey(),
                    lockedPayment.getIdempotencyKey(), /// [추가] 멱등키 전달
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




