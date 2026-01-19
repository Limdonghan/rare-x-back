package com.project.rare_x_back.common;

import com.project.rare_x_back.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;


public class CustomUserDetails implements UserDetails {

        private final User user; // 엔티티

        public CustomUserDetails(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            // 사용자의 권한을 반환 (예: ROLE_ADMIN, ROLE_USER)
            return Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        @Override
        public String getPassword() { return user.getPassword(); }

        @Override
        public String getUsername() { return user.getEmail(); }

        @Override public boolean isAccountNonExpired() {
            return true; }

        @Override public boolean isAccountNonLocked() {
            return true; }

        @Override public boolean isCredentialsNonExpired() {
            return true; }

        @Override public boolean isEnabled() {
            return true; }

    }




