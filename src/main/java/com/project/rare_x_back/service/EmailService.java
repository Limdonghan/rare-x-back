package com.project.rare_x_back.service;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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

    private static final String EMAIL_PREFIX = "email:";
    private static final String VERIFIED_PREFIX = "verified:";  // 추가!

    private static final long CODE_EXPIRATION_MINUTES = 5;


    //  이메일 인증번호 발송
    @Async  ///  별도 스레드에서 실행 (응답을 기다리지 않음)
    public void sendVerificationCode(String email) {

        // 1. 6자리 인증번호 생성
        String code = generateCode();

        // 2. Redis에 저장 (5분 유효)
        String key = EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 3. 이메일 발송
        try {
            SimpleMailMessage message = getSimpleMailMessage(email, code);

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

    @NonNull
    private SimpleMailMessage getSimpleMailMessage(String email, String code) {
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
        return message;
    }

    //  이메일 인증번호 검증
    public boolean verifyCode(String email, String code) {
        String key = EMAIL_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.delete(key);  // 인증번호 삭제

            // 인증 완료 표시 (10분간 유효)
            String verifiedKey = VERIFIED_PREFIX + email;
            redisTemplate.opsForValue().set(verifiedKey, "true", 10, TimeUnit.MINUTES);

            return true;
        }
        return false;
    }


    // 이메일 인증 여부 확인
    public boolean isVerified(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        return redisTemplate.hasKey(verifiedKey);
    }

    //  6자리 랜덤 인증번호 생성
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}