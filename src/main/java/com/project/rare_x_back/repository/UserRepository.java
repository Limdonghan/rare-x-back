package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.ProviderType;
import com.project.rare_x_back.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    // 삭제되지 않은 유저 전체 조회
    List<User> findAllByIsDeletedFalse();

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

    // ====== 관리자 회원 관리 (MANAGER-004) ======

    // 전체 조회 (탈퇴 회원 포함)
    @Query("SELECT u FROM User u")
    Page<User> findAllForAdmin(Pageable pageable);

    // 상태 필터
    Page<User> findByStatusIn(List<Status> statuses, Pageable pageable);

    // 가입유형 필터
    Page<User> findByProviderType(ProviderType providerType, Pageable pageable);

    // 상태 + 가입유형 필터
    Page<User> findByStatusInAndProviderType(
            List<Status> statuses, ProviderType providerType, Pageable pageable);

    // 키워드 검색
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 통계
    @Query("SELECT u.status, COUNT(u) FROM User u GROUP BY u.status")
    List<Object[]> countByStatus();
}
