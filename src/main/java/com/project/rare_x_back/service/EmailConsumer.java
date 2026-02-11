package com.project.rare_x_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.dto.request.EmailMessageRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailConsumer {

    private final JavaMailSender javaMailSender;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "email-topic",groupId = "${spring.kafka.consumer.group-id}")
    public void consumeEmailEvent(String jsonMessage){
        try {
            /// json 문자열을 네가 만든 dto 객체로 변환
            EmailMessageRequestDto messageRequestDto = objectMapper.readValue(jsonMessage, EmailMessageRequestDto.class);

            /// 실제 매일 세팅 및 발송
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(messageRequestDto.getTo());
            mailMessage.setSubject(messageRequestDto.getTitle());
            mailMessage.setText(messageRequestDto.getBody());

            javaMailSender.send(mailMessage);

        } catch (JsonMappingException e) {
            throw new RuntimeException("카프카 메일 메핑 에러: ",e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("카프카 메일 파싱 에러: ",e);
        }
    }
}
