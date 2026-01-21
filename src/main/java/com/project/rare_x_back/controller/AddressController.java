package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.AddressRegisterRequestDto;
import com.project.rare_x_back.dto.request.DefaultAddressUpdateRequestDto;
import com.project.rare_x_back.dto.request.UserAddressUpdateRequestDto;
import com.project.rare_x_back.dto.response.JusoResponseDto;
import com.project.rare_x_back.dto.response.UserAddressResponseDto;
import com.project.rare_x_back.service.AddressService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/mypage/addresses")
public class AddressController {

    private final AddressService addressService;

    // 주소 등록
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> createAddress (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressRegisterRequestDto requestDto
    ) {
        addressService.createAddress(userDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success("신규 주소 등록이 완료 되었습니다."));
    }

    // 주소 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<JusoResponseDto>> searchAddress(
            @RequestParam(name = "keyword") String keyword,
            @RequestParam(name = "currentPage", defaultValue = "1") int currentPage) {
        JusoResponseDto response = addressService.searchAddress(keyword, currentPage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //주소 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAddressResponseDto>>> getAllAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<UserAddressResponseDto> response = addressService.getAllAddress(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 주소 수정 (받는 사람, 상세주소만)
    @PatchMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> updateAddressInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressUpdateRequestDto requestDto
    ) {
        addressService.updateAddress(userDetails.getUserId(), addressId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("주소 정보가 수정되었습니다."));
    }

    // 기본 배송지 여부 수정 (정책 적용)
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<Void>> updateDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId,
            @RequestBody DefaultAddressUpdateRequestDto requestDto
    ) {
        addressService.updateDefaultAddress(userDetails.getUserId(), addressId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("기본 배송지 설정이 변경되었습니다."));
    }

    // 주소 삭제
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId
    ) {
        addressService.deleteAddress(userDetails.getUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("배송지 삭제가 완료 되었습니다."));
    }
}
