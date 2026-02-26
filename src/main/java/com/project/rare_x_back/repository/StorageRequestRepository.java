package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.StorageRequest;
import com.project.rare_x_back.enums.StorageRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StorageRequestRepository extends JpaRepository<StorageRequest, Long> {

    // 사용자의 특정 상태 보관 신청 목록 조회
    @Query("SELECT sr FROM StorageRequest sr " +
            "JOIN FETCH sr.product p " +
            "JOIN FETCH p.brand " +
            "LEFT JOIN FETCH p.images " +
            "WHERE sr.user.userId = :userId AND sr.status = :status")
    Page<StorageRequest> findByUserIdAndStatus(@Param("userId") Long userId,
                                               @Param("status") StorageRequestStatus status, Pageable pageable);

    // 사용자의 전체 보관 신청 목록 조회
    @Query("SELECT sr FROM StorageRequest sr " +
            "JOIN FETCH sr.product p " +
            "JOIN FETCH p.brand " +
            "LEFT JOIN FETCH p.images " +
            "WHERE sr.user.userId = :userId")
    Page<StorageRequest> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 본인 확인용 단건 조회
    @Query("SELECT sr FROM StorageRequest sr " +
            "JOIN FETCH sr.product p " +
            "JOIN FETCH p.brand " +
            "JOIN FETCH sr.user " +
            "WHERE sr.storageRequestId = :id")
    Optional<StorageRequest> findByIdWithDetails(@Param("id") Long id);

    // 회원탈퇴 - 진행 중 보관 신청 존재 여부
    boolean existsByUser_UserIdAndStatusIn(Long userId, List<StorageRequestStatus> statuses);
}