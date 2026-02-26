package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.UpdateBidPriceRequestDto;
import com.project.rare_x_back.dto.response.MyBuyBidMatchedResponseDto;
import com.project.rare_x_back.dto.response.MyBuyBidResponseDto;
import com.project.rare_x_back.dto.response.MySaleBidMatchedResponseDto;
import com.project.rare_x_back.dto.response.MySaleBidResponseDto;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.service.BidService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/mypage/bids")
public class UserBidsController {
    private final BidService bidService;

    // 구매 입찰 목록 조회
    @GetMapping("/buybid")
    public ResponseEntity<ApiResponse<Page<MyBuyBidResponseDto>>> getMyBuyBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) List<BidStatus> statuses){
        Page<MyBuyBidResponseDto> myBuyBids = bidService.getMyBuyBids(userDetails.getEmail(), statuses, pageable);
        return ResponseEntity.ok(ApiResponse.success(myBuyBids));
    }

    // 판매 입찰 목록 조회
    @GetMapping("/salebid")
    public ResponseEntity<ApiResponse<Page<MySaleBidResponseDto>>> getMySaleBids (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) List<BidStatus> statuses) {
        Page<MySaleBidResponseDto> response = bidService.getMySaleBids(userDetails.getEmail(), statuses, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 판매 입찰 목록 조회 - 체결됨
    @GetMapping("/salebid/matched")
    public ResponseEntity<ApiResponse<Page<MySaleBidMatchedResponseDto>>> getMySaleBidMatched(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String orderStatus) {
        Page<MySaleBidMatchedResponseDto> response = bidService.getMySaleBidMatched(
                userDetails.getEmail(), orderStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 구매 입찰 목록 조회 - 체결됨
    @GetMapping("/buybid/matched")
    public ResponseEntity<ApiResponse<Page<MyBuyBidMatchedResponseDto>>> getMyBuyBidMatched(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String orderStatus) {
        Page<MyBuyBidMatchedResponseDto> result = bidService.getMyBuyBidMatched(userDetails.getEmail(), orderStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 구매 입찰가 수정
    @PatchMapping("/buybid/{buyId}")
    public ResponseEntity<ApiResponse<Void>> updateMyBuyBids (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long buyId,
            @Valid @RequestBody UpdateBidPriceRequestDto dto
            ) {
        bidService.updateBuyBidPrice(userDetails.getUserId(), buyId, dto);
        return ResponseEntity.ok(ApiResponse.success("구매 입찰가 수정이 완료되었습니다."));
    }

    // 판매 입찰가 수정
    @PatchMapping("/salebid/{sellId}")
    public ResponseEntity<ApiResponse<Void>> updateMySaleBids (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sellId,
            @Valid @RequestBody UpdateBidPriceRequestDto dto
    ) {
        bidService.updateSaleBidPrice(userDetails.getUserId(), sellId, dto);
        return ResponseEntity.ok(ApiResponse.success("판매 입찰가 수정이 완료되었습니다."));
    }

    // 구매 입찰 취소
    @DeleteMapping("/buybid/{buyId}")
    public ResponseEntity<ApiResponse<Void>> cancelBuyBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long buyId
    ){
        bidService.cancelBuyBid(userDetails.getUserId(), buyId);
        return ResponseEntity.ok(ApiResponse.success("구매 입찰이 취소 되었습니다."));
    }

    // 판매 입찰 취소
    @DeleteMapping("/salebid/{sellId}")
    public ResponseEntity<ApiResponse<Void>> cancelSaleBid (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sellId
    ) {
        bidService.cancelSaleBid(userDetails.getUserId(), sellId);
        return ResponseEntity.ok(ApiResponse.success("판매 입찰이 취소되었습니다."));
    }

}
