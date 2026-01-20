package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.AddressRegisterRequestDto;
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

    // 주소 수정

    // 주소 삭제
}
