package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.SignUpRequest;
import com.project.rare_x_back.dto.response.SignUpResponse;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional
    public SignUpResponse signUP(SignUpRequest request) {

        // 1. 비밀번호 일치 검증
        if
    }
}
