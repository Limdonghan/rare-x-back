package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.WalletHistory;
import com.project.rare_x_back.enums.WalletHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {

    @Query("""
            select h from WalletHistory h
            join fetch h.wallet w
            where h.wallet.walletId= :walletId
            and h.type = :type
            order by h.createdAt desc
""")
    List<WalletHistory> findAllByWalletId(Long walletId, WalletHistoryType type);

}
