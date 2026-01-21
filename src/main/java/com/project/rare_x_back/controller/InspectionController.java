package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.InspectionChecklistRequestDto;
import com.project.rare_x_back.dto.request.InspectionFailRequestDto;
import com.project.rare_x_back.dto.response.InspectionChecklistResponseDto;
import com.project.rare_x_back.dto.response.InspectionHistoryDetailResponseDto;
import com.project.rare_x_back.dto.response.InspectionHistoryResponseDto;
import com.project.rare_x_back.dto.response.InspectionResponseDto;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inspections")
public class InspectionController {

    private final InspectionService inspectionService;

    // 전체 검수 목록 조회 (보관 + 주문, 페이징)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InspectionResponseDto>>> getAllInspections(
            @RequestParam(required = false) InspectionStatus status,
            Pageable pageable) {

        Page<InspectionResponseDto> response = inspectionService.getAllInspections(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 보관 검수 목록 조회 (페이징)
    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<Page<InspectionResponseDto>>> getStorageInspections(
            @RequestParam(required = false) InspectionStatus status,
            Pageable pageable) {

        Page<InspectionResponseDto> response = inspectionService.getInspectionList(InspectionType.STORAGE, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 주문 검수 목록 조회 (페이징)
    @GetMapping("/order")
    public ResponseEntity<ApiResponse<Page<InspectionResponseDto>>> getOrderInspections(
            @RequestParam(required = false) InspectionStatus status,
            Pageable pageable) {

        Page<InspectionResponseDto> response = inspectionService.getInspectionList(InspectionType.ORDER, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 검수 상세 조회
    @GetMapping("/{inspectionId}")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> getInspection(
            @PathVariable Long inspectionId) {

        InspectionResponseDto response = inspectionService.getInspection(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 도착 확인 (SHIPPED_TO_WAREHOUSE → PENDING_INSPECTION)
    @PatchMapping("/{inspectionId}/confirm-arrival")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> confirmArrival(
            @PathVariable Long inspectionId) {

        InspectionResponseDto response = inspectionService.confirmArrival(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response, "도착 확인이 완료되었습니다"));
    }

    // 검수 시작 (PENDING_INSPECTION → INSPECTING)
    @PatchMapping("/{inspectionId}/start")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> startInspection(
            @PathVariable Long inspectionId,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {

        InspectionResponseDto response = inspectionService.startInspection(inspectionId, adminDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "검수가 시작되었습니다"));
    }

    // 체크리스트 조회
    @GetMapping("/{inspectionId}/checklist")
    public ResponseEntity<ApiResponse<InspectionChecklistResponseDto>> getChecklist(
            @PathVariable Long inspectionId) {

        InspectionChecklistResponseDto response = inspectionService.getChecklist(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response, "체크리스트 조회 성공"));
    }

    // 체크리스트 수정
    @PatchMapping("/{inspectionId}/checklist")
    public ResponseEntity<ApiResponse<InspectionChecklistResponseDto>> updateChecklist(
            @PathVariable Long inspectionId,
            @Valid @RequestBody InspectionChecklistRequestDto request) {

        InspectionChecklistResponseDto response = inspectionService.updateChecklist(inspectionId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "체크리스트 수정 성공"));
    }

    // 검수 합격 처리 (INSPECTING → PASSED)
    @PatchMapping("/{inspectionId}/pass")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> passInspection(
            @PathVariable Long inspectionId) {

        InspectionResponseDto response = inspectionService.passInspection(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response, "검수 합격 처리 완료"));
    }

    // 검수 불합격 처리 (INSPECTING → FAILED)
    @PatchMapping("/{inspectionId}/fail")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> failInspection(
            @PathVariable Long inspectionId,
            @Valid @RequestBody InspectionFailRequestDto request) {

        InspectionResponseDto response = inspectionService.failInspection(inspectionId, request.getFailReason());
        return ResponseEntity.ok(ApiResponse.success(response, "검수 불합격 처리 완료"));
    }

    // 검수 이력 목록 조회 (페이징)
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<InspectionHistoryResponseDto>>> getInspectionHistory(
            @RequestParam(required = false) InspectionStatus status,
            Pageable pageable) {

        Page<InspectionHistoryResponseDto> response = inspectionService.getInspectionHistory(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 검수 이력 상세 조회 (체크리스트 포함)
    @GetMapping("/history/{inspectionId}")
    public ResponseEntity<ApiResponse<InspectionHistoryDetailResponseDto>> getInspectionHistoryDetail(
            @PathVariable Long inspectionId) {

        InspectionHistoryDetailResponseDto response = inspectionService.getInspectionHistoryDetail(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}