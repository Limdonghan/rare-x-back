package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.UpdateBidPriceRequestDto;
import com.project.rare_x_back.dto.response.MyBuyBidMatchedResponseDto;
import com.project.rare_x_back.dto.response.MyBuyBidResponseDto;
import com.project.rare_x_back.dto.response.MySaleBidMatchedResponseDto;
import com.project.rare_x_back.dto.response.MySaleBidResponseDto;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.enums.CurrentStatus;
import com.project.rare_x_back.service.BidService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<ApiResponse<List<MyBuyBidResponseDto>>> getMyBuyBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) BidStatus status){
        List<MyBuyBidResponseDto> response = bidService.getMyBuyBids(userDetails.getEmail(), status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 판매 입찰 목록 조회
    @GetMapping("/salebid")
    public ResponseEntity<ApiResponse<List<MySaleBidResponseDto>>> getMySaleBids (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) BidStatus status) {
        List<MySaleBidResponseDto> response = bidService.getMySaleBids(userDetails.getEmail(), status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 판매 입찰 목록 조회 - 체결됨
    @GetMapping("/salebid/matched")
    public ResponseEntity<ApiResponse<List<MySaleBidMatchedResponseDto>>> getMySaleBidMatched(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String orderStatus) {
        List<MySaleBidMatchedResponseDto> response = bidService.getMySaleBidMatched(
                userDetails.getEmail(), orderStatus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 구매 입찰 목록 조회 - 체결됨
    @GetMapping("/buybid/matched")
    public ResponseEntity<ApiResponse<List<MyBuyBidMatchedResponseDto>>> getMyBuyBidMatched(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String orderStatus) {
        List<MyBuyBidMatchedResponseDto> result = bidService.getMyBuyBidMatched(userDetails.getUsername(), orderStatus);
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
