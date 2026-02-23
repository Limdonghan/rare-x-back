package com.project.rare_x_back.repository;


import com.project.rare_x_back.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
public interface AdminDashboardRepository extends JpaRepository<PaymentHistory, Long> {
    // (1) 구매 순수수료: commission_fee - (commission_fee * cancel_amount/total_amount)
    @Query(value = """
        SELECT
          COALESCE(SUM(
            ph.commission_fee
            -
            (
              ph.commission_fee
              * COALESCE(pc.cancel_amount,0)
              / ph.total_amount
            )
          ),0) AS net_buyer_fee
        FROM payment_histories ph
        JOIN payments p ON ph.order_id = p.order_id
        LEFT JOIN (
            SELECT payment_id, SUM(cancel_amount) AS cancel_amount
            FROM payment_cancels
            GROUP BY payment_id
        ) pc ON pc.payment_id = p.payment_id
        WHERE ph.status = 'COMPLETE'
          AND DATE(ph.res_date) = :date
        """, nativeQuery = true)
    Long sumNetBuyerFeeByDate(@Param("date") String date);

    // (2) 판매 수수료: settlements commission_fee 합
    @Query(value = """
        SELECT COALESCE(SUM(s.commission_fee),0)
        FROM settlements s
        WHERE s.status = 'COMPLETE'
          AND DATE(s.completed_at) = :date
        """, nativeQuery = true)
    Long sumSellerFeeByDate(@Param("date") String date);

    // (3) 일별 구매 순수수료 (그래프용)
    @Query(value = """
        SELECT
          DATE(ph.res_date) AS d,
          COALESCE(SUM(
            ph.commission_fee
            -
            (
              ph.commission_fee
              * COALESCE(pc.cancel_amount,0)
              / ph.total_amount
            )
          ),0) AS net_buyer_fee
        FROM payment_histories ph
        JOIN payments p ON ph.order_id = p.order_id
        LEFT JOIN (
            SELECT payment_id, SUM(cancel_amount) AS cancel_amount
            FROM payment_cancels
            GROUP BY payment_id
        ) pc ON pc.payment_id = p.payment_id
        WHERE ph.status = 'COMPLETE'
          AND DATE(ph.res_date) BETWEEN :startDate AND :endDate
        GROUP BY DATE(ph.res_date)
        ORDER BY d
        """, nativeQuery = true)
    List<Map<String, Object>> sumNetBuyerFeeDaily(@Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);

    // (4) 일별 판매 수수료 (그래프용)
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
    List<Map<String, Object>> sumSellerFeeDaily(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate);
}
