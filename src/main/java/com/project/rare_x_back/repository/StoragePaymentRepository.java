package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.StoragePayment;
import com.project.rare_x_back.enums.StoragePaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoragePaymentRepository extends JpaRepository<StoragePayment, Long> {

    /**
     * 특정 보관함의 결제 내역 조회
     */
    List<StoragePayment> findByStorageItem_StorageId(Long storageId);

    /**
     * 특정 사용자의 결제 내역 조회
     */
    List<StoragePayment> findByUser_UserId(Long userId);

    /**
     * 재시도 대상 조회: 특정 상태 & retry_count < 3
     * user, storageItem 함께 로딩 (N+1 방지)
     */
    @EntityGraph(attributePaths = {"user", "storageItem"})
    List<StoragePayment> findByStatusAndRetryCountLessThan(
            StoragePaymentStatus status,
            int maxRetry
    );

    /**
     * 중복 생성 방지: 특정 보관함의 특정 기간 결제 존재 여부
     */
    boolean existsByStorageItem_StorageIdAndBillingPeriodStart(
            Long storageId,
            LocalDate periodStart
    );

    /**
     * 특정 보관함의 가장 최근 성공한 결제 조회
     */
    Optional<StoragePayment> findTopByStorageItem_StorageIdAndStatusOrderByBillingPeriodEndDesc(
            Long storageId,
            StoragePaymentStatus status
    );
}