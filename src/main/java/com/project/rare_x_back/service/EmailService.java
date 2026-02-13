package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.EmailType;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String VERIFIED_PREFIX = "verified:";// 추가!
    public static final String TEMP_PASSWORD_PREFIX = "temp_password:";
    public static final String EMAIL_PREFIX = "email:";

    private static final long CODE_EXPIRATION_MINUTES = 5;
    //임시 비밀번호 사용자 플래그 30분(로그인 후 에도 비번 변경까지 유지)
    public static final String TEMP_PASSWORD_FLAG_PREFIX = "temp_password_flag:";
    private static final String TEMP_PASSWORD_LIMIT_PREFIX = "temp_password_limit:";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    //  이메일 인증번호 발송
    @Async  ///  별도 스레드에서 실행 (응답을 기다리지 않음)
    public void sendVerificationCode(String email) {

        // 1. 6자리 인증번호 생성
        String code = generateCode();

        // 2. Redis에 저장 (5분 유효)
        String key = EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 3. 이메일 발송 요청 (Kafka) - Title/Body 생성은 Consumer로 위임
        try {
            emailProducer.sendEmail(email, EmailType.VERIFICATION, code);

            log.info("===========================================");
            log.info("이메일 발신 요청 (Kafka) - VERIFICATION");
            log.info("수신자: {}", email);
            log.info("인증번호: {}", code);
            log.info("===========================================");

        } catch (Exception e) {
            try{
                /// Kafka 전송 실패 시, 이미 저장된 인증번호를 삭제하여 일관성 유지
                redisTemplate.delete(key);
            } catch (Exception ex){
                log.warn("Kafka 실패 후 Redis 인증번호 삭제 중 오류 발생: {}", ex.getMessage());
            }
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

    @Transactional
    public void sendTempPassword(String email) {
        //  유저 확인
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 중복 발급 방지
        String limitKey = TEMP_PASSWORD_LIMIT_PREFIX + email;

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(limitKey, "sent", 1, TimeUnit.HOURS);
        // setIfAbsent는 레디스 서버에서 조회 후 없으면 저장을 하나의 명령어로 실행

        if (Boolean.FALSE.equals(isFirstRequest)) {
            throw new CustomException(ErrorCode.TEMP_PASSWORD_ALREADY_SENT);
        }

        // 임시 비번 생성
        String tempPassword = generateTempPassword();

        // DB에 임시 비번 저장
        String encodedPassword = passwordEncoder.encode(tempPassword);
        user.updatePassword(encodedPassword);

        // 레디스에 임시비번 사용자 플래그 설정 7일
        String flagKey = TEMP_PASSWORD_FLAG_PREFIX + email;
        redisTemplate.opsForValue().set(flagKey, "true", 7, TimeUnit.DAYS);


        // 이메일 발송
        try {
            emailProducer.sendEmail(email, EmailType.TEMP_PASSWORD, tempPassword);
            log.info("임시 비밀번호 발급 완료: email={}", email);
        } catch (Exception e) {
            log.error("이메일 발송 실패: email={}, error={}", email, e.getMessage());
            // 이메일 발송 실패 시 레디스에서 데이터 삭제
            redisTemplate.delete(flagKey);
            redisTemplate.delete(limitKey);
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    // 임시 비밀번호 사용자인지 확인
    public boolean isTempPasswordUser(String email) {
        String flagKey = TEMP_PASSWORD_FLAG_PREFIX + email;
        String flag = redisTemplate.opsForValue().get(flagKey);
        return "true".equals(flag);
    }

    // 임시 비밀번호 플래그 삭제 (비밀번호 변경 완료 시)
    public void clearTempPasswordFlag(String email) {
        String flagKey = TEMP_PASSWORD_FLAG_PREFIX + email;
        redisTemplate.delete(flagKey);
        log.info("임시 비밀번호 플래그 삭제: email={}", email);
    }

}