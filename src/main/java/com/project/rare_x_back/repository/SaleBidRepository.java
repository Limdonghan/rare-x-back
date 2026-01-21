package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.enums.BidStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface SaleBidRepository extends JpaRepository<SaleBid, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SaleBid> findAllByProductAndPriceAndStatusOrderByCreatedAtAsc(Product productId, int price, BidStatus status);

}
