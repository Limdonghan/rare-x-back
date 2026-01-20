package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.enums.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleBidRepository extends JpaRepository<SaleBid, Long> {


    Optional<List<SaleBid>> findAllByProductIdAndPriceAndStatusOrderByCreatedAtAsc(Product productId, int price, BidStatus status);

}
