package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.LoginRequest;
import com.project.rare_x_back.dto.request.SignUpRequest;
import com.project.rare_x_back.dto.response.LoginResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor    // final 필드를 가진 생성자 만들기
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  final JwtTokenProvider jwtTokenProvider;

    // 회원등록
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
                .status(Status.PENDING)
                .isDeleted(false)
                .build();

        userRepository.save(user);

        // 4. 응답 DTO 생성 (빌더 패턴)
        SignUpResponse response = SignUpResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .message("회원가입이 완료되었습니다")
                .build();

        return response;
    }

    // 로그인
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 탈퇴한 회원 확인
        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.ACCOUNT_DELETED);
        }

        // 3. 비밀번호 확인 (BCrypt)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 4. 계정 활성화 확인 (PENDING 상태 허용)
        // 나중에 이메일 인증 추가 시 ACTIVE만 로그인 가능하도록 변경
        if (user.getStatus() == Status.BANNED || user.getStatus() == Status.BLOCKED) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // 5. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getRole().name()
        );

        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getUserId()
        );

        // 6. 응답 생성
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
