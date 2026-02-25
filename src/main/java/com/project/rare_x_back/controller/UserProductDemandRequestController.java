package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.ProductDemandRequestDto;
import com.project.rare_x_back.dto.response.ProductDemandRequestResponseDto;
import com.project.rare_x_back.service.ProductDemandRequestService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserProductDemandRequestController {

    private final ProductDemandRequestService demandRequestService;

    @PostMapping("/demand-requests")
    public ResponseEntity<ApiResponse<ProductDemandRequestResponseDto>> createDemandRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart MultipartFile image,
            @Valid @RequestPart ProductDemandRequestDto requestDto
            ) {
        ProductDemandRequestResponseDto response =  demandRequestService.createDemandRequest(userDetails.getUserId(), requestDto, image);
        return  ResponseEntity.ok(ApiResponse.success(response));
    }

}
