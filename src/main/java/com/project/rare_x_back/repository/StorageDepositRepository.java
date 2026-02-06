package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.StorageDeposit;
import com.project.rare_x_back.enums.StoragePaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorageDepositRepository extends JpaRepository<StorageDeposit, Long> {

    // 보관 신청 ID로 보증금 조회
    Optional<StorageDeposit> findByStorageRequest_StorageRequestId(Long storageRequestId);

    // 중복 결제 방지: 해당 신청에 성공한 보증금이 있는지
    boolean existsByStorageRequest_StorageRequestIdAndStatus(Long storageRequestId, StoragePaymentStatus status);
}