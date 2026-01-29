package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 동기화용 - N+1 방지
    @EntityGraph(attributePaths = {
            "buyer",
            "seller",
            "product"
    })
    @Query("SELECT o FROM Order o")
    List<Order> findAllForSync();
}
