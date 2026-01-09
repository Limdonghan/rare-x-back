package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.BillingKeyRequestDto;
import com.project.rare_x_back.entity.BillingKey;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.repository.BillingKeyRepository;
import com.project.rare_x_back.repository.PaymentRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

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

            /// 4. DB 저장
            BillingKey build = BillingKey.builder()
                    .user(user)
                    .billingKey(billingKey)
                    .cardCompany(cardInfo.get("company").toString())
                    .cardNumber(String.valueOf(cardInfo.get("number")))
                    .build();

            log.info("카드 등록 완료 - User: {}, Company: {}", user.getName(), cardInfo.get("company"));
            return billingKeyRepository.save(build);

        } catch (Exception e) {
            log.error("결제 승인 실패: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

}




