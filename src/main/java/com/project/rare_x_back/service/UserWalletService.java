package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.entity.UserWallet;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWalletService {

    private final UserWalletRepository userWalletRepository;


    // 정산 완료 → 판매자 지갑에 금액 적립
    @Transactional
    public void depositSettlementAmount(User seller, long settleAmount) {
        // 지갑 조회 -> 기존 방식에서 회원 가입 시 빈지갑 생성으로 변경함
        UserWallet wallet = userWalletRepository.findByUser(seller)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "사용자의 지갑을 찾을 수 없습니다."));

        wallet.increase(settleAmount);
    }

}
