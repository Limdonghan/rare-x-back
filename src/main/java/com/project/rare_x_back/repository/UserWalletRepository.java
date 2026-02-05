package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.entity.UserWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    Optional<UserWallet> findByUser(User user);

    // 같은 시간에 정산 보상, 충전 동시에 들어올 수 있기 때문에.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from UserWallet w where w.user.userId = :userId")
    UserWallet findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("select w from UserWallet w where w.user.userId = :userId")
    Optional<UserWallet> findByUser_UserId(Long userId);
}
