package com.project.rare_x_back.service;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.username}")  // ← 추가!
    private String fromEmail;

    private static final String EMAIL_PREFIX = "email:verify:";
    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRATION_MINUTES = 5;


    //  이메일 인증번호 발송
    public void sendVerificationCode(String email) {

        // 1. 6자리 인증번호 생성
        String code = generateCode();

        // 2. Redis에 저장 (5분 유효)
        String key = EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 3. 이메일 발송
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);  // ← 추가! (발신자 설정)
            message.setTo(email);
            message.setSubject("[RARE-X] 이메일 인증번호");
            message.setText(
                    "안녕하세요. RARE-X입니다.\n\n" +
                            "회원가입을 위한 인증번호는 다음과 같습니다.\n\n" +
                            "인증번호: " + code + "\n\n" +
                            "인증번호는 " + CODE_EXPIRATION_MINUTES + "분간 유효합니다.\n" +
                            "본인이 요청하지 않았다면 이 메일을 무시하세요."
            );

            mailSender.send(message);

            log.info("===========================================");
            log.info("이메일 발송 성공");
            log.info("발신자: {}", fromEmail);
            log.info("수신자: {}", email);
            log.info("인증번호: {}", code);
            log.info("===========================================");

        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    //  이메일 인증번호 검증
    public boolean verifyCode(String email, String code) {

        String key = EMAIL_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(key);

        // 인증번호 확인
        if (savedCode == null) {
            return false;
        }

        // 인증 성공 시 Redis에서 삭제
        if (savedCode.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    //  6자리 랜덤 인증번호 생성
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}