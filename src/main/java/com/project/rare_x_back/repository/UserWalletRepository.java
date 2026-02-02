package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    Optional<UserWallet> findByUser(User user);

    User user(User user);
}
