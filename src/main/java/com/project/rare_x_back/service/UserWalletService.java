package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.entity.UserWallet;
import com.project.rare_x_back.repository.UserWalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserWalletService {

    private final UserWalletRepository userWalletRepository;


    /**
     * 정산 지갑 보장
     * - 지갑 있으면 반환
     * - 없으면 생성
     * - 동시성 상황에서도 단 1개만 생성됨
     */
    @Transactional
    public UserWallet getOrCreateWallet(User user) {
        return userWalletRepository.findByUser(user)
                .orElseGet(() -> {
                    try {
                        return userWalletRepository.save(UserWallet.create(user));
                    } catch (DataIntegrityViolationException e) {
                        // 동시에 다른 트랜잭션이 먼저 생성한 경우
                        return userWalletRepository.findByUser(user)
                                .orElseThrow(() ->
                                        new IllegalStateException("지갑 생성 충돌 후 재조회 실패")
                                );
                    }
                });
    }

    // 정산 완료 → 판매자 지갑에 금액 적립
    @Transactional
    public void depositSettlementAmount(User seller, long settleAmount) {
        UserWallet wallet = getOrCreateWallet(seller);
        wallet.increase(settleAmount);
    }

}
