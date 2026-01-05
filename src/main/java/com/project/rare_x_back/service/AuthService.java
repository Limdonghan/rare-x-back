package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.SignUpRequest;
import com.project.rare_x_back.dto.response.SignUpResponse;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.ProviderType;
import com.project.rare_x_back.enums.Role;
import com.project.rare_x_back.enums.Status;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor    // final 필드를 가진 생성자 만들기
public class AuthService {

    private final UserRepository userRepository;

    // 회원등록
    @Transactional
    public SignUpResponse signUP(SignUpRequest request) {

         // 1. 비밀번호 일치 검증
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        // 2. 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 가입된 이메일입니다");
        }

        // 3. User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())    // 나중에 암호화 추가해야함
                .name(request.getName())
                .phone(request.getPhone().replaceAll("-", ""))  // 하이픈 제거
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
}
