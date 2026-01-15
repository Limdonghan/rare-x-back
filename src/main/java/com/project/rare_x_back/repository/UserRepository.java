package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일로 존재 여부 확인 (boolean 타입은 null 불가능, Optional 감쌀 필요 없음)
    boolean existsByEmail(String email);

    // 삭제되지 않은 유저만 조회
    Optional<User> findByUserIdAndIsDeletedFalse(Long userId);
}
