package com.project.rare_x_back.common;

import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;


public class CustomUserDetails implements UserDetails {

        private final Long userId;
        private final String email;
        private final String password;
        private final Role role;


        //로그인 시: User 엔티티 정보를 기반으로 생성
        public CustomUserDetails(User user) {
            this.userId = user.getUserId();
            this.email = user.getEmail();
            this.password = user.getPassword();
            this.role = user.getRole();
        }

        // JWT 필터용: 토큰에서 추출한 최소 정보로 생성
        // password는 인증 완료 후 세션에 저장되는 용도이므로 필터 단계에서는 null가능
        public CustomUserDetails(Long userId, String email, String password, String role) {
            this.userId = userId;
            this.email = email;
            this.password = password;
            this.role = Role.valueOf(role);
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            // 사용자의 권한을 반환 (예: ROLE_ADMIN, ROLE_USER)
            return Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role.name()));
        }

        @Override
        public String getPassword() { return this.password != null ? this.password : ""; }

        @Override
        public String getUsername() { return this.email; }  //우리 프로젝트는 아이디 = 이메일

    // 계정 상태 설정 (기본값 true)
        @Override public boolean isAccountNonExpired() {
            return true; }

        @Override public boolean isAccountNonLocked() {
            return true; }

        @Override public boolean isCredentialsNonExpired() {
            return true; }

        @Override public boolean isEnabled() {
            return true; }

    }




