package com.project.rare_x_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.dto.request.EmailMessageRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendEmail(String to, String title, String body) {
        try {
            EmailMessageRequestDto emailMessageRequestDto = EmailMessageRequestDto.builder()
                    .to(to)
                    .title(title)
                    .body(body)
                    .build();

            /// 객체를 json 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(emailMessageRequestDto);

            /// 카프카 전송
            kafkaTemplate.send("email-topic", jsonMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카프카 메일 메세지 변환 실패",e);
        }
    }
}
