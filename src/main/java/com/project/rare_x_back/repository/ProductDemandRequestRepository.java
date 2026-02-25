package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.ProductDemandRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductDemandRequestRepository extends JpaRepository<ProductDemandRequest, Long> {

    // 기간 필터
    Page<ProductDemandRequest> findByCreatedAtAfter(
            LocalDateTime dateTime,
            Pageable pageable
    );


    List<ProductDemandRequest> findByCreatedAtBefore(
            LocalDateTime dateTime
    );
}
