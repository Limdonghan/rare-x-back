package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.BuyBidResponseDto;
import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.BuyBidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyBidService {

    private final BuyBidRepository buyBidRepository;

    /**
     * 구매 입찰 목록 조회 (페이징)
     */
    public Page<BuyBidResponseDto> getBuyBids(String email, BidStatus status, Pageable pageable) {
        Page<BuyBid> buyBids;

        if (status == null) {
            buyBids = buyBidRepository.findByUserEmail(email, pageable);
        } else {
            buyBids = buyBidRepository.findByUserEmailAndStatus(email, status, pageable);
        }

        return buyBids.map(BuyBidResponseDto::from);
    }

    /**
     * 구매 입찰 취소
     */
    @Transactional
    public BuyBidResponseDto cancelBuyBid(String email, Long buyId) {
        BuyBid buyBid = buyBidRepository.findById(buyId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "입찰 정보를 찾을 수 없습니다."));

        // 본인 확인
        if (!buyBid.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 입찰만 취소할 수 있습니다.");
        }

        // 상태 확인 (OPEN 상태만 취소 가능)
        if (buyBid.getStatus() != BidStatus.OPEN) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "진행 중인 입찰만 취소할 수 있습니다.");
        }

        buyBid.statusUpdate(BidStatus.CANCELED);

        return BuyBidResponseDto.from(buyBid);
    }
}