package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.PaymentCancel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {

    boolean existsByPayment_PaymentId(Long paymentId);
}
