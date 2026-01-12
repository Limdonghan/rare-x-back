package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.StorageRequestCreateDto;
import com.project.rare_x_back.dto.response.StorageRequestResponseDto;
import com.project.rare_x_back.service.StorageRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageRequestController {

    private final StorageRequestService storageRequestService;


    // 보관 판매 신청
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<StorageRequestResponseDto>> createStorageRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StorageRequestCreateDto request) {

        StorageRequestResponseDto response = storageRequestService.createStorageRequest(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "보관 신청이 완료되었습니다"));
    }
}
