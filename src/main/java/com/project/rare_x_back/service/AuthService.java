package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.LoginResponse;
import com.project.rare_x_back.dto.response.RefreshTokenResponse;
import com.project.rare_x_back.dto.response.SignUpResponse;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.ProviderType;
import com.project.rare_x_back.enums.Role;
import com.project.rare_x_back.enums.Status;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor    // final 필드를 가진 생성자 만들기
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    private final TokenBlacklistService tokenBlacklistService;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    // 회원가입
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

         // 1. 비밀번호 일치 검증
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 3. User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .providerType(ProviderType.LOCAL)
                .role(Role.USER)
                .status(Status.PENDING)  // 이메일 인증 전 상태
                .isDeleted(false)
                .build();

        userRepository.save(user);

        // 4. 응답 DTO 생성
        return SignUpResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .message("회원가입이 완료되었습니다.  이메일 인증을 진행해주세요.")
                .build();
    }

    // 이메일 인증번호 발송
    @Transactional(readOnly = true)
    public void sendVerificationCode(EmailSendRequest request) {

        // 1. 사용자 존재 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 이미 인증된 경우
        if (user.getStatus() == Status.ACTIVE) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // 3. 인증번호 발송
        emailService.sendVerificationCode(request.getEmail());
    }

    // 이메일 인증
    @Transactional
    public void verifyEmail(EmailVerifyRequest request) {

        // 1. 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 인증번호 검증
        boolean isValid = emailService.verifyCode(request.getEmail(), request.getCode());

        if (!isValid) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 3. 상태 변경 (PENDING → ACTIVE) // 회원가입 할 때 인증하고 로그인하면 필요없음(나중에 DB 수정하면 바꿔야함. 1/7 피드백)
        user.setStatus(Status.ACTIVE);
    }

    // 로그인
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 탈퇴한 회원 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        // 3. 비밀번호 확인 (BCrypt)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 4. 계정 활성화 확인
        if (user.getStatus() != Status.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 5. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // 6. Refresh Token을 Redis에 저장 (7일)
        String key = REFRESH_TOKEN_PREFIX + user.getUserId();
        redisTemplate.opsForValue().set(key, refreshToken, 7, TimeUnit.DAYS);

        // 7. 응답 생성
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 로그아웃 (Refresh Token 삭제 + Access Token 블랙리스트)
    @Transactional
    public void logout(Long userId, String accessToken) {
        // 1. Refresh Token 삭제
        String refreshKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(refreshKey);

        // 2. Access Token 블랙리스트에 추가
        long expiration = jwtTokenProvider.getExpiration(accessToken);
        tokenBlacklistService.addToBlacklist(accessToken, expiration);
    }

    // Refresh Token으로 Access Token 갱신
    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        // 1. Refresh Token 유효성 검증 (JWT 자체)
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Refresh Token에서 userId 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());

        // 3. Redis에서 저장된 Refresh Token 확인
        String key = REFRESH_TOKEN_PREFIX + userId;
        String savedRefreshToken = redisTemplate.opsForValue().get(key);

        // 4. Redis에 없거나 불일치 → 무효한 토큰
        if (savedRefreshToken == null || !savedRefreshToken.equals(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 5. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 6. 탈퇴한 사용자 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        // 7. 계정 상태 확인
        if (user.getStatus() != Status.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 8. 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);

        return  RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .build();
    }
}
