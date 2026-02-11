package com.project.rare_x_back.service;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailProducer emailProducer; // 추가

    @Value("${spring.mail.username}")  // ← 추가!
    private String fromEmail;

    private static final String EMAIL_PREFIX = "email:";
    private static final String VERIFIED_PREFIX = "verified:";// 추가!
    public static final String TEMP_PASSWORD_PREFIX = "temp_password:";

    private static final long CODE_EXPIRATION_MINUTES = 5;
    //임시 비밀번호 사용자 플래그 30분(로그인 후 에도 비번 변경까지 유지)
    public static final String TEMP_PASSWORD_FLAG_PREFIX = "temp_password_flag:";


    //  이메일 인증번호 발송
    @Async  ///  별도 스레드에서 실행 (응답을 기다리지 않음)
    public void sendVerificationCode(String email) {

        // 1. 6자리 인증번호 생성
        String code = generateCode();

        // 2. Redis에 저장 (5분 유효)
        String key = EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 3. 이메일 발송 (Kafka Producer 호출)
        try {
            String title = "[RARE-X] 이메일 인증번호";
            String body = "안녕하세요. RARE-X입니다.\n\n" +
                    "회원가입을 위한 인증번호는 다음과 같습니다.\n\n" +
                    "인증번호: " + code + "\n\n" +
                    "인증번호는 " + CODE_EXPIRATION_MINUTES + "분간 유효합니다.\n" +
                    "본인이 요청하지 않았다면 이 메일을 무시하세요.";

            emailProducer.sendEmail(email, title, body);

            log.info("===========================================");
            log.info("이메일 발신 요청 (Kafka)");
            log.info("수신자: {}", email);
            log.info("인증번호: {}", code);
            log.info("===========================================");

        } catch (Exception e) {
            log.error("이메일 발송 요청 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
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


    // ===========================================
    // ----PASSWORD LESS----
    // ===========================================

    // 임시 비밀번호 생성
    private String generateTempPassword() {
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$^&*";

        String allChars = lowerCase + numbers + specialChars;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        //각 타입별 최소 1개씩 (대문자 제외)
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        //나머지 6자리 랜덤 생성
        for (int i = 4; i < 10; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        //문자열 섞기
        List<Character> chars = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(chars, random);

        return chars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }



    // 임시비밀번호 Redis 저장, 이메일 발송
    public String sendTempPassword(String email) {
        //임시 비번 생성
        String tempPassword = generateTempPassword();

        //레디스 저장(30분간)
        String key =TEMP_PASSWORD_PREFIX + email;
        redisTemplate.opsForValue().set(key, tempPassword, 30, TimeUnit.MINUTES);

        log.info("임시 비밀번호 Redis 저장: email={}, key= {}, 유효시간 = 30분", email, key);
        log.info("임시 비밀번호 : {}", tempPassword);

        try {
            sendMailTempPassword(email, tempPassword);

            log.info("==============");
            log.info("이메일 발송 성공");
            log.info("==============");

            return tempPassword;

        } catch (Exception e) {
            log.error("이메일 발송 실패 : {} ", e.getMessage());
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
    @Async("taskExecutor")
    public void sendMailTempPassword(String email, String tempPassword) {
        String title = "[RARE-X] 패스워드리스 서비스 해지 임시 비밀번호 발송";
        String body = "안녕하세요. RARE-X 입니다.\n\n" +
                "패스워드리스 서비스 해지 후 로그인을 위한 인증 번호는 다음과 같습니다.\n\n" +
                "임시 비밀번호: " + tempPassword + "\n\n";

        emailProducer.sendEmail(email, title, body);
    }

    //임시 비밀번호 검증 및 삭제
    public void verifyAndConsumeTempPassword(String email, String inputPassword) {
        String key = getTempPasswordKey(email);
        String storedTempPassword = redisTemplate.opsForValue().get(key);

        if (storedTempPassword == null) {
            log.warn("임시 비밀번호 없음 또는 만료: email={}", email);
            return;
        }

        if (storedTempPassword.equals(inputPassword)) {
            // 임시 비밀번호 맞음 → Redis에서 삭제하고 플래그 설정
            redisTemplate.delete(key);

            // 비밀번호 변경 강제 플래그 설정 (30분)
            String flagKey = getTempPasswordFlagKey(email);
            redisTemplate.opsForValue().set(flagKey, "true", 30, TimeUnit.MINUTES);

            log.info("임시 비밀번호 검증 성공 및 플래그 설정: email={}", email);
        }

    }

    // 임시 비밀번호 사용자인지 확인
    public boolean isTempPasswordUser(String email) {
        String flagKey = getTempPasswordFlagKey(email);
        String flag = redisTemplate.opsForValue().get(flagKey);
        return "true".equals(flag);
    }

    //임시 비밀번호 플래그 삭제
    public void clearTempPasswordFlag(String email) {
        String flagKey = getTempPasswordFlagKey(email);
        redisTemplate.delete(flagKey);
        log.info("임시 비밀번호 플래그 삭제: email={}", email);
    }


    public static String getTempPasswordKey(String email) {
        return TEMP_PASSWORD_PREFIX + email;
    }

    public static String getTempPasswordFlagKey(String email) {
        return TEMP_PASSWORD_FLAG_PREFIX + email;
    }

}