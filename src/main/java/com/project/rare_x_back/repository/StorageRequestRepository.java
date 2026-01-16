package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.StorageRequest;
import com.project.rare_x_back.enums.StorageRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorageRequestRepository extends JpaRepository<StorageRequest, Long> {

    // 사용자의 특정 상태 보관 신청 목록 조회
    @Query("SELECT sr FROM StorageRequest sr " +    // 1. 보관 신청(StorageRequest) 테이블에서 데이터를 선택해 가져온다
            "JOIN FETCH sr.product p " +            // 2. 신청 정보와 연결된 상품(product) 정보를 한꺼번에 묶어서 가져온다
            "JOIN FETCH p.brand " +                 // 3. 그 상품의 브랜드(brand) 정보까지 한 번에 다 가져온다
            "WHERE sr.user.userId = :userId " +     // 4. 조건: 요청한 사용자의 ID가 입력받은 값과 일치해야 한다
            "AND sr.status = :status " +            // 5. 조건: 신청 상태(예: 검수대기 등)가 입력받은 값과 일치해야 한다
            "ORDER BY sr.createdAt ASC")            // 6. 정렬: 신청일시를 기준으로 오래된 순서(오름차순)로 나열한다
    List<StorageRequest> findByUserIdAndStatus(@Param("userId") Long userId,
                                               @Param("status")StorageRequestStatus status);

    // 본인 확인용 단건 조회
    @Query("SELECT sr FROM StorageRequest sr " +     // 1. 보관 신청(StorageRequest) 테이블에서 데이터를 선택해 가져온다
            "JOIN FETCH sr.product p " +              // 2. 신청 정보와 연결된 상품(product) 정보를 한꺼번에 묶어서 가져온다
            "JOIN FETCH p.brand " +                   // 3. 그 상품의 브랜드(brand) 정보까지 한 번에 다 가져온다
            "JOIN FETCH sr.user " +                   // 4. 신청한 사용자(user)의 정보까지 한꺼번에 묶어서 가져온다
            "WHERE sr.storageRequestId = :id")        // 5. 조건: 찾고자 하는 특정 보관 신청의 번호(ID)가 일치해야 한다
    Optional<StorageRequest> findByIdWithDetails(@Param("id") Long id);
}
