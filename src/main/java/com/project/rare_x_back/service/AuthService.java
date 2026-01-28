package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.LoginResponseDto;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.dto.response.RefreshTokenResponseDto;
import com.project.rare_x_back.dto.response.SignUpResponseDto;
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
    private final PasswordlessService passwordlessService;
    private final SearchService searchService;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    // 회원가입
    @Transactional
    public SignUpResponseDto signUp(SignUpRequestDto request) {

        // 1. 이메일 인증 여부 확인 추가
        if (!emailService.isVerified(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 2. 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 3. 비밀번호 확인
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .providerType(ProviderType.LOCAL)
                .role(Role.USER)
                .status(Status.ACTIVE)
                .isDeleted(false)
                .build();

        userRepository.save(user);
        searchService.indexUser(user);  // Typesense 인덱싱 추가

        // 4. 응답 DTO 생성
        return SignUpResponseDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    // 인증번호 발송 (회원가입 전 이메일 인증용)
    public void sendVerificationCode(EmailSendRequestDto request) {
        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 인증번호 발송
        emailService.sendVerificationCode(request.getEmail());
    }

    // 이메일 인증 (인증번호 확인만)
    @Transactional
    public void verifyEmail(EmailVerifyRequestDto request) {
        // 인증번호 검증
        boolean isValid = emailService.verifyCode(request.getEmail(), request.getCode());

        if (!isValid) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
    }

    // 로그인
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {

        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 탈퇴한 회원 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        // 3. 패스워드리스 사용자 확인 (일반 로그인 차단)
        if (user.getPasswordlessEnabled()) {
            throw new CustomException(ErrorCode.PASSWORDLESS_USER_CANNOT_LOGIN);
        }

        // 일반 비번(또는 DB에 저장된 임시비번) 일치 확인
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        boolean requiresChange = false;

        if (passwordMatches) {
            // 비번이 맞다면 이게 임시 비번상태인지 확인
            // 이때는 레디스에 저장된 임시 비밀번호 원문과 비교
            String tempPasswordKey = EmailService.getTempPasswordKey(user.getEmail());
            String storedTemp = redisTemplate.opsForValue().get(tempPasswordKey);

            if (storedTemp != null && storedTemp.equals(request.getPassword())) {
                // 입력한 비번이 레디스에 저장된 임시비번과 일치한다면
                requiresChange = true;

                // 사용했으니 레디스에서 임시 비번 삭제하고 비번 변경 필요 플래그 생성
                emailService.verifyAndConsumeTempPassword(user.getEmail(), request.getPassword());
                log.info("임시 비밀번호 로그인 성공 (DB 일치): email={}", user.getEmail());
            } else {
                // 레디스에 임시비번이 없거나 일반 비번인 경우
                // 기존에 로그인해서 생성된 플래그가 있는지도 한번 더 체크
                requiresChange = emailService.isTempPasswordUser(user.getEmail());
                log.info("일반 비밀번호 로그인 성공: email={}, 변경필요={}", user.getEmail(), requiresChange);
            }
        } else {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 5. 계정 활성화 확인
        if (user.getStatus() != Status.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 5. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole().name(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getRole().name());

        // 7. Refresh Token을 Redis에 저장 (7일)
        String key = REFRESH_TOKEN_PREFIX + user.getUserId();
        redisTemplate.opsForValue().set(key, refreshToken, 7, TimeUnit.DAYS);

        // [추가] 패스워드리스용 토큰 발급
        PasswordlessResponseDto passwordlessResponseDto = passwordlessService.verifyManagementAccess(request.getEmail(), request.getPassword());

        // 7. 응답 생성
        // 8. 응답 생성 (임시 비밀번호 여부포함 추가)

        log.info("로그인 성공: email={}", user.getEmail());

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .name(user.getName())
                .passwordlessToken(passwordlessResponseDto.getData())
                .role(user.getRole().name())
                .isPasswordChangeRequired(requiresChange) //임시비번 여부 반영
                .build();
    }

    // 로그아웃 (Refresh Token 삭제 + Access Token 블랙리스트)
    @Transactional
    public void logout(String userEmail, String accessToken) {
        // 이메일로 유저 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. Refresh Token 삭제
        String refreshKey = REFRESH_TOKEN_PREFIX + user.getUserId();
        redisTemplate.delete(refreshKey);

        // 2. Access Token 블랙리스트에 추가
        long expiration = jwtTokenProvider.getExpiration(accessToken);
        tokenBlacklistService.addToBlacklist(accessToken, expiration);
    }

    // Refresh Token으로 Access Token 갱신
    @Transactional(readOnly = true)
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request) {

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
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, user.getRole().name(), user.getEmail());

        return  RefreshTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .build();
    }

    //임시 비밀번호 유저일 경우 비밀번호 변경
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto request) {

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 임시 비밀번호 사용자 확인
        boolean isTempPasswordUser = emailService.isTempPasswordUser(user.getEmail());

        // 3. 현재 비밀번호 확인 (임시 비밀번호 사용자는 생략 가능)
        if (!isTempPasswordUser) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new CustomException(ErrorCode.CURRENT_PASSWORD_REQUIRED);
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
        }

        // 4. 새 비밀번호 확인
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 5. 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        // 6. 비밀번호 업데이트
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodedNewPassword);

        // 7. 임시 비밀번호 플래그 삭제 (Redis)
        if (isTempPasswordUser) {
            emailService.clearTempPasswordFlag(user.getEmail());
        }

        log.info("비밀번호 변경 완료: userId={}, wasTempPassword={}",
                userId, isTempPasswordUser);
    }

}
