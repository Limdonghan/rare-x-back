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
     * 민감한 내용(인증번호, 임시비밀번호 등)은 직접 Payload에 포함하지 않고, 이메일 주소(Key)와 타입(Type)만 전송합니다.
     * 실제 내용은 Consumer가 수신 시 Redis에서 조회하여 조합됩니다.
     *
     * @param to   수신자 이메일 주소
     * @param type 이메일 타입 (VERIFICATION: 인증번호, TEMP_PASSWORD: 임시비밀번호)
     */
    public void sendEmail(String to, EmailType type) {
        try {
            EmailMessageRequestDto emailMessageRequestDto = EmailMessageRequestDto.builder()
                    .to(to)
                    .type(type)
                    .build();

            /// 객체를 json 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(emailMessageRequestDto);

            /// 카프카 전송 (비동기 콜백 처리)
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(emailTopic, jsonMessage);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Kafka 전송 성공 - Topic: {}, Partition: {}, Offset: {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Kafka 전송 실패: {}", ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카프카 메일 메시지 변환 실패", e);
        }
    }
}
