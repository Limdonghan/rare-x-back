package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.StorageRequestCreateDto;
import com.project.rare_x_back.dto.response.StorageItemResponseDto;
import com.project.rare_x_back.dto.response.StorageRequestResponseDto;
import com.project.rare_x_back.service.StorageItemService;
import com.project.rare_x_back.service.StorageRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageRequestController {

    private final StorageRequestService storageRequestService;
    private final StorageItemService storageItemService;


    // 보관 판매 신청
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<StorageRequestResponseDto>> createStorageRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StorageRequestCreateDto request) {

        StorageRequestResponseDto response = storageRequestService.createStorageRequest(userDetails.getUsername(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "보관 신청이 완료되었습니다"));
    }

    // 보관 중 상품 목록 조회
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<StorageItemResponseDto>>> getMyStorageItems(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<StorageItemResponseDto> response = storageItemService.getMyStorageItems(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 발송 대기 목록 조회 (PENDING 상태)
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<StorageRequestResponseDto>>> getPendingStorageRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<StorageRequestResponseDto> response = storageRequestService.getPendingStorageRequests(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 발송 처리
    @PostMapping("/{storageRequestId}/ship")
    public ResponseEntity<ApiResponse<StorageRequestResponseDto>> shipToWarehouse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storageRequestId) {

        StorageRequestResponseDto response = storageRequestService.shipToWarehouse(userDetails.getUsername(), storageRequestId);

        return ResponseEntity.ok(ApiResponse.success(response, "발송 처리가 완료되었습니다"));
    }
}
