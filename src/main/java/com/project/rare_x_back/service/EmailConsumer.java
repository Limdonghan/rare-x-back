package com.project.rare_x_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.dto.request.EmailMessageRequestDto;
import com.project.rare_x_back.enums.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumer {

    private final JavaMailSender javaMailSender;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate; // 추가

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Kafka 'email-topic'을 구독하여 이메일 발송 이벤트를 처리합니다.
     * 1. JSON 메시지를 Deserialization하여 DTO로 변환합니다.
     * 2. 이메일 타입(Type)에 따라 Redis에서 실제 데이터(인증번호/임시비밀번호)를 조회합니다.
     * 3. 데이터가 유효하면 이메일 본문을 생성하여 JavaMailSender로 발송합니다.
     *
     * 에러 처리:
     * - JSON 파싱 에러(JsonProcessingException): 복구 불가능하므로 로그를 남기고 메시지를 Skip합니다.
     * - 기타 예외: 일시적인 오류일 가능성이 있으므로 로그를 남깁니다. (추후 Dead Letter Queue 적용 가능)
     *
     * @param jsonMessage Kafka로부터 수신한 JSON 형식의 메시지
     */
    @KafkaListener(topics = "${spring.kafka.topic.email}",groupId = "${spring.kafka.consumer.group-id}")
    public void consumeEmailEvent(String jsonMessage){
        try {
            /// json 문자열을 DTO로 변환
            EmailMessageRequestDto messageRequestDto = objectMapper.readValue(jsonMessage, EmailMessageRequestDto.class);
            String email = messageRequestDto.getTo();
            EmailType type = messageRequestDto.getType();
            
            String title;
            String body;

            if (type == EmailType.VERIFICATION) {
                String key = EmailService.EMAIL_PREFIX + email;
                String code = redisTemplate.opsForValue().get(key);
                if (code == null) {
                    log.warn("인증번호 만료 또는 없음 (Skip): email={}", email);
                    return;
                }
                title = "[RARE-X] 이메일 인증번호";
                body = "안녕하세요. RARE-X입니다.\n\n" +
                        "회원가입을 위한 인증번호는 다음과 같습니다.\n\n" +
                        "인증번호: " + code + "\n\n" +
                        "인증번호는 5분간 유효합니다.\n" +
                        "본인이 요청하지 않았다면 이 메일을 무시하세요.";

            } else if (type == EmailType.TEMP_PASSWORD) {
                String key = EmailService.TEMP_PASSWORD_PREFIX + email;
                String tempPassword = redisTemplate.opsForValue().get(key);
                if (tempPassword == null) {
                    log.warn("임시 비밀번호 만료 또는 없음 (Skip): email={}", email);
                    return;
                }
                title = "[RARE-X] 패스워드리스 서비스 해지 임시 비밀번호 발송";
                body = "안녕하세요. RARE-X 입니다.\n\n" +
                        "패스워드리스 서비스 해지 후 로그인을 위한 인증 번호는 다음과 같습니다.\n\n" +
                        "임시 비밀번호: " + tempPassword + "\n\n";
            } else {
                log.warn("알 수 없는 이메일 타입 (Skip): {}", type);
                return;
            }

            /// 실제 메일 세팅 및 발송
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(email);
            mailMessage.setSubject(title);
            mailMessage.setText(body);

            javaMailSender.send(mailMessage);
            log.info("이메일 발송 성공 (Kafka Consumer): email={}, type={}", email, type);

        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 파싱 에러 (Skip): payload={}, error={}", jsonMessage, e.getMessage());
            /// 예외를 던지지 않아 메시지를 Skip 처리 (오프셋 커밋됨)
        } catch (Exception e) {
            log.error("이메일 발송 실패 (Retry 가능성 있음): {}", e.getMessage());
            /// 일시적인 메일 서버 오류 등은 재시도 할 수 있도록 예외를 던질 수 있음
            /// 다만, 무한 재시도를 방지하려면 별도의 ErrorHandler 설정이 필요함.
            /// 여기서는 일단 로그 찍고 넘어가는 방식으로 처리 (안정성 우선)
        }
    }
}
