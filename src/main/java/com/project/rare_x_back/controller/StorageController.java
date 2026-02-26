package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.StorageRequestCreateDto;
import com.project.rare_x_back.dto.response.StorageItemResponseDto;
import com.project.rare_x_back.dto.response.StorageRequestResponseDto;
import com.project.rare_x_back.enums.StorageRequestStatus;
import com.project.rare_x_back.service.StorageItemService;
import com.project.rare_x_back.service.StorageRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

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

    // 보관 신청 목록 조회 (전체 또는 상태별 필터링)
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<Page<StorageRequestResponseDto>>> getMyStorageRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) StorageRequestStatus status) {

        Page<StorageRequestResponseDto> response = storageRequestService.getMyStorageRequests(userDetails.getUsername(), status, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 보관 중 상품 목록 조회
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<StorageItemResponseDto>>> getMyStorageItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "storedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<StorageItemResponseDto> response = storageItemService.getMyStorageItems(userDetails.getUsername(), pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 발송 대기 목록 조회 (PENDING 상태)
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<StorageRequestResponseDto>>> getPendingStorageRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<StorageRequestResponseDto> response = storageRequestService.getPendingStorageRequests(userDetails.getUsername(), pageable);

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

    // 보관 신청 취소
    @PatchMapping("/{storageRequestId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelStorageRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storageRequestId) {

        storageRequestService.cancelStorageRequest(userDetails.getUsername(), storageRequestId);

        return ResponseEntity.ok(ApiResponse.success("보관 신청이 취소되고 보증금이 환불되었습니다."));
    }

    // 보관 상품 반송 요청 (고객 요청)
    @PostMapping("/items/{storageId}/release")
    public ResponseEntity<ApiResponse<Void>> requestRelease(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storageId) {

        storageItemService.requestRelease(userDetails.getUsername(), storageId);

        return ResponseEntity.ok(ApiResponse.success("반송 요청이 완료되었습니다."));
    }
}