package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일로 존재 여부 확인 (boolean 타입은 null 불가능, Optional 감쌀 필요 없음)
    boolean existsByEmail(String email);

    // 삭제되지 않은 유저만 조회 (ID기준)
    Optional<User> findByUserIdAndIsDeletedFalse(Long userId);

    // 삭제되지 않은 유저만 조회 (Email 기준)
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    //비밀번호 업데이트
    @Modifying
    @Query("update User u set u.password = :password where u.email = :email")
    void updatePasswordByEmail(@Param("email") String email,
                               @Param("password") String password);

    //패스워드리스 상태 업데이트
    @Modifying
    @Query("UPDATE User u SET u.passwordlessEnabled = :enabled " +
            "WHERE u.email = :email")
    void updatePasswordlessStatus(@Param("email") String email,
                                  @Param("enabled") Boolean enabled);
}
