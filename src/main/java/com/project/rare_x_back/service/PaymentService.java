package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.AutoPaymentRequestDto;
import com.project.rare_x_back.dto.request.BillingKeyRequestDto;
import com.project.rare_x_back.entity.BillingKey;
import com.project.rare_x_back.entity.Payment;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.repository.BillingKeyRepository;
import com.project.rare_x_back.repository.PaymentRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final WebClient webClient;
    private final PaymentRepository paymentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final UserRepository userRepository;


    /**
     * 카드 등록 (빌링키 발급)
     * 프론트에서 받은 authKey로 실제 결제 가능한 billingKey를 받아와 저장합니다.
     */
    @Transactional
    public BillingKey registerCard(BillingKeyRequestDto billingKeyRequestDto,String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

        /// 3. Billing_Kye DB에 기존에 등록한 유저가 있다면 삭제 후 갱신 (또는 추가)
        billingKeyRepository.findByUser(user).ifPresent(billingKey -> {
                    log.info("기존 빌링키 삭제: User {}", user.getUserId());
                    billingKeyRepository.delete(billingKey);
                }
        );


        try {
            /// 1. 토스에 빌링키 발급 요청
            Map response = webClient.post()                     /// POST 요청
                    .uri("billing/authorizations/issue")    /// 엔드포인트 설정
                    .bodyValue(Map.of(
                            "authKey", billingKeyRequestDto.getAuthKey(),
                            "customerKey", billingKeyRequestDto.getCustomerKey()
                    ))                                          /// Request Body 설정
                    .retrieve()                                 /// 실제 HTTP 요청 실행
                    .bodyToMono(Map.class)                     /// 응답을 Map으로 반환
                    .block();                                   /// 동기식으로 대기

            // TODO: Order 조회 및 검증 로직 추가 (DB의 주문 금액과 일치하는지 등)

            /// 2. 응답에서 필요한 정보 추출
            String billingKey = (String) response.get("billingKey");
            Map cardInfo = (Map) response.get("card");

            /// Response값 매핑
            String customerKey = String.valueOf(response.get("customerKey"));
            String cardCompany = String.valueOf(response.get("cardCompany"));
            String cardNumber = String.valueOf(cardInfo.get("number")).substring(12,16);
//            OffsetDateTime authenticatedAt = (OffsetDateTime) response.get("authenticatedAt");
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

            log.info("카드 등록 완료 - User: {}, Company: {}", user.getName(), cardInfo.get("company"));
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
    public Payment payWithBillingKey (AutoPaymentRequestDto autoPaymentRequestDto, String email) {
        /// 1. 유저 확인
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

        /// 2. DB에서 저장된 빌링키 꺼내오기
        BillingKey billingKey = billingKeyRepository.findByUser(user).orElseThrow(() -> new RuntimeException("등록되지 않은 빌링키"));

        try {
            /// 3. 토스 API 호출
            Map response = webClient.post()
                    .uri("billing/" + billingKey.getBillingKey())
                    .bodyValue(Map.of(
                            "amount", autoPaymentRequestDto.getAmount(),
                            "customerKey", billingKey.getCustomerKey(),
                            "orderId", autoPaymentRequestDto.getOrderId(),
                            "orderName", autoPaymentRequestDto.getOrderName()
                    ))                                          /// Request Body 설정
                    .retrieve()                                 /// 실제 HTTP 요청 실행
                    .bodyToMono(Map.class)                      /// 응답을 Map으로 반환
                    .block();                                   /// 동기식으로 대기


            /// API에서 값 꺼내서 Enum으로 변환 (Mapping)

            String method = String.valueOf(response.get("method"));
            String status = String.valueOf(response.get("status"));
            OffsetDateTime requestedAt = OffsetDateTime.parse((String) response.get("requestedAt"));

            /// 4. 결제 성공 후 DB에 결제내역 저장 (Payment 엔티티)
            Payment build = Payment.builder()
                    .orderId(1L)
                    .tossOrderId(autoPaymentRequestDto.getOrderId())
                    .tossPaymentKey(autoPaymentRequestDto.getOrderId())
                    .amount(autoPaymentRequestDto.getAmount())
                    .tossPaymentMethod(method)
                    .tossPaymentStatus(status)
                    .requestedAt(requestedAt)
                    .build();

            log.info("자동 결제 성공 - User: {}, Amount: {}", user.getName(), autoPaymentRequestDto.getAmount());

            return paymentRepository.save(build);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

    }



}




