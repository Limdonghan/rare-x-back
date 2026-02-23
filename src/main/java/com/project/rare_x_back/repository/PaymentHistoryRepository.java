package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    Optional<PaymentHistory> findByOrder_OrderId(Long orderId);

    // 오늘 구매 수수료 합
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
            ),0)
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
    Long sumNetBuyerFee(@Param("date") String date);
}
