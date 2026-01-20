package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
