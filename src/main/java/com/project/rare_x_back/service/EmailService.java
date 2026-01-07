package com.project.rare_x_back.service;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)  // ← required = false 추가!
    private JavaMailSender mailSender;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMAIL_PREFIX = "email:verify:";
    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRATION_MINUTES = 5;

    /**
     * 이메일 인증번호 발송
     */
    public void sendVerificationCode(String email) {

        // 1. 6자리 인증번호 생성
        String code = generateCode();

        // 2. Redis에 저장 (5분 유효)
        String key = EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 3. 콘솔에 출력 (개발 중)
        log.info("===========================================");
        log.info("이메일 인증번호 발송");
        log.info("이메일: {}", email);
        log.info("인증번호: {}", code);
        log.info("유효시간: {}분", CODE_EXPIRATION_MINUTES);
        log.info("===========================================");

        // 4. 이메일 발송 (mailSender가 있을 때만)
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
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
                log.info("이메일 발송 성공: {}", email);

            } catch (Exception e) {
                log.error("이메일 발송 실패: {}", e.getMessage());
                // 이메일 발송 실패해도 계속 진행 (개발 중)
            }
        } else {
            log.info("Mail 설정이 없어 이메일 미발송 (콘솔로 확인하세요)");
        }
    }

    /**
     * 이메일 인증번호 검증
     */
    public boolean verifyCode(String email, String code) {

        String key = EMAIL_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(key);

        // 인증번호 확인
        if (savedCode == null) {
            return false;  // 만료되었거나 존재하지 않음
        }

        // 인증 성공 시 Redis에서 삭제
        if (savedCode.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    /**
     * 6자리 랜덤 인증번호 생성
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);  // 100000 ~ 999999
        return String.valueOf(code);
    }
}