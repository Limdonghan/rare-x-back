package com.project.rare_x_back.repository;


import com.project.rare_x_back.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
public interface AdminDashboardRepository extends JpaRepository<PaymentHistory, Long> {

    /**
     * (1) 구매 순수수료: commission_fee - (commission_fee * cancel_amount/total_amount)
     * 당일 매출에 대한 수수료 계산 (순이익)
     * <br>
     * [DB 튜닝]
     * DATE() 함수 제거 및 구간(>=, <) 조건으로 인덱스 범위 검색 적용
     * 스칼라 서브쿼리 사용으로 payment_cancels 전체 데이터 무지성 GROUP BY(메모리 낭비) 방지
     * */
    @Query(value = """
            SELECT
                  COALESCE(SUM(
                    ph.commission_fee
                    -
                    (
                      ph.commission_fee
                      * COALESCE((SELECT SUM(cancel_amount) FROM payment_cancels pc WHERE pc.payment_id = p.payment_id), 0)
                      / ph.total_amount
                    )
                  ),0) AS net_buyer_fee
                FROM payment_histories ph
                JOIN payments p ON ph.order_id = p.order_id
                WHERE ph.status = 'COMPLETE'
                  AND ph.res_date >= :date
                  AND ph.res_date < DATE_ADD(:date, INTERVAL 1 DAY)
        """, nativeQuery = true)
    Long sumNetBuyerFeeByDate(@Param("date") String date);

    /**
     * (2) 판매 수수료: settlements commission_fee 합
     * <br>
     * [DB튜닝]
     * DATE() 함수 제거 및 구간(>=, <) 조건으로 인덱스 범위 검색 적용 (Full Table Scan 방지)
     * */
    @Query(value = """
        SELECT COALESCE(SUM(s.commission_fee),0)
        FROM settlements s
        WHERE s.status = 'COMPLETE'
          AND s.completed_at >= :date
          AND s.completed_at < DATE_ADD(:date, INTERVAL 1 DAY);
        """, nativeQuery = true)
    Long sumSellerFeeByDate(@Param("date") String date);

    /**
     * (3) 일별 구매 순수수료 (그래프용)
     *  <br>
     * [DB튜닝]
     * DATE() 함수 제거 및 구간(>=, <) 조건으로 메인 테이블 인덱스 범위 검색 적용
     * 스칼라 서브쿼리(DEPENDENT SUBQUERY) 사용으로 DERIVED(임시 테이블) 메모리 부하 차단
     * */
    @Query(value = """
            SELECT
                DATE(ph.res_date) AS d,
                COALESCE(SUM(
                    ph.commission_fee
                    -
                    (
                    ph.commission_fee
                        * COALESCE((SELECT SUM(cancel_amount) AS cancel_amount
                                    FROM payment_cancels pc
                                    WHERE pc.payment_id = p.payment_id),0)
                        / ph.total_amount
                    )
                ),0) AS net_buyer_fee
                FROM payment_histories ph
                JOIN payments p ON ph.order_id = p.order_id
                WHERE ph.status = 'COMPLETE'
                AND ph.res_date >= :startDate -- DATE() 함수 제거, 시작일
                AND ph.res_date < DATE_ADD(:endDate, INTERVAL 1 DAY) -- 종료일 + 1일 미만
                GROUP BY DATE(ph.res_date)
                ORDER BY d
        """, nativeQuery = true)
    List<Map<String, Object>> sumNetBuyerFeeDaily(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);


    /**
     * (4) 일별 판매 수수료 (그래프용)
     * <br>
     * [DB튜닝] : DATE() 함수 제거 및 구간(>=, <) 조건으로 인덱스 범위 검색 적용 (Using temporary 부하 최소화)
     * */
    @Query(value = """
        SELECT
          DATE(s.completed_at) AS d,
          COALESCE(SUM(s.commission_fee),0) AS seller_fee
        FROM settlements s
        WHERE s.status = 'COMPLETE'
          AND DATE(s.completed_at) BETWEEN :startDate AND :endDate
        GROUP BY DATE(s.completed_at)
        ORDER BY d
        """, nativeQuery = true)
    List<Map<String, Object>> sumSellerFeeDaily(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
