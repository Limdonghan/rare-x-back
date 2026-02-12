package com.project.rare_x_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.dto.request.EmailMessageRequestDto;
import com.project.rare_x_back.enums.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.email}")
    private String emailTopic;

    /**
     * Kafka로 이메일 발송 요청을 비동기적으로 전송합니다.
     * 민감한 내용(인증번호, 임시비밀번호 등)은 암호화하여 Payload에 포함합니다.
     *
     * @param to   수신자 이메일 주소
     * @param type 이메일 타입 (VERIFICATION: 인증번호, TEMP_PASSWORD: 임시비밀번호)
     * @param content 이메일 본문에 포함될 내용 (인증코드 또는 임시비밀번호)
     */
    public void sendEmail(String to, EmailType type, String content) {
        try {
            EmailMessageRequestDto emailMessageRequestDto = EmailMessageRequestDto.builder()
                    .to(to)
                    .type(type)
                    .content(content)
                    .build();

            /// 객체를 json 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(emailMessageRequestDto);

            /// 카프카 전송 및 결과 *동기* 대기 (10초 타임아웃 등 설정 가능하지만 여기선 기본 get 사용)
            /// get()을 호출하면 전송이 완료될 때까지 기다리며, 실패 시 예외를 던집니다.
            kafkaTemplate.send(emailTopic, jsonMessage).get();

            log.info("Kafka 전송 성공: email={}, type={}", to, type);

        } catch (Exception e) {
            log.error("Kafka 전송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 발송 시스템 오류 (Kafka)", e);
        }
    }
}
