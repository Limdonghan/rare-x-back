package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.InspectionResponseDto;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.service.AdminService;
import com.project.rare_x_back.service.InspectionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final InspectionService inspectionService;

    //상품 등록
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Void>> createProduct(@Valid @RequestBody ProductCreateRequestDto productCreateRequestDto) {
        // 서비스 호출 후 생성된 상품의 ID를 반환받음
        adminService.createProduct(productCreateRequestDto);
        // 성공 응답(201 Created)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("상품 등록이 완료되었습니다."));
    }

    //상품 정보 수정
    @PatchMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequestDto productUpdateRequestDto
    ) {
        adminService.updateProduct(productUpdateRequestDto, productId);
        return ResponseEntity.ok(ApiResponse.success("상품 정보 수정이 완료되었습니다."));
    }

    //상품 삭제
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct (@PathVariable Long productId) {
        adminService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("상품 삭제가 완료되었습니다."));
    }


    //카테고리 추가
    @PostMapping("/categories")
    public ResponseEntity <ApiResponse<Void>> createCategory(@Valid @RequestBody CategoryCreateRequestDto categoryCreateRequestDto){
        adminService.createCategory(categoryCreateRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("카테고리 등록이 완료되었습니다."));
    }

    //카테고리 수정
    @PatchMapping("/categories/{categoryId}")
    public ResponseEntity <ApiResponse<Void>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequestDto categoryUpdateRequestDto) {
        adminService.updateCategory(categoryUpdateRequestDto, categoryId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("카테고리 정보 수정이 완료되었습니다."));
    }

    //카테고리 삭제
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory (@PathVariable Long categoryId) {
        adminService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("카테고리 삭제가 완료되었습니다."));
    }

    //브랜드 추가
    @PostMapping("/brands")
    public ResponseEntity createBrand(@Valid @RequestBody BrandCreateRequestDto brandCreateRequestDto){
        adminService.createBrand(brandCreateRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("브랜드 등록이 완료되었습니다."));
    }

    @PatchMapping("/brands/{brandId}")
    public ResponseEntity <ApiResponse<Void>> updateBrand(
            @PathVariable Long brandId,
            @Valid @RequestBody BrandUpdateRequestDto brandUpdateRequestDto) {
        adminService.updateBrand(brandUpdateRequestDto, brandId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("브랜드 정보 수정이 완료되었습니다."));
    }

    // 전체 검수 목록 조회 (보관 + 주문)
    @GetMapping("/inspections")
    public ResponseEntity<ApiResponse<List<InspectionResponseDto>>> getAllInspections(
            @RequestParam(required = false) InspectionStatus status) {

        List<InspectionResponseDto> response = inspectionService.getAllInspections(status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보관 검수 목록 조회
     * - status 없으면 전체 조회
     * - status 있으면 해당 상태만 조회 (PENDING_INSPECTION, INSPECTING, PASSED, FAILED)
     */
    @GetMapping("/inspections/storage")
    public ResponseEntity<ApiResponse<List<InspectionResponseDto>>> getStorageInspections(
            @RequestParam(required = false)InspectionStatus status) {   // required = false : 사용자가 필터를 선택하면 필터링된 값을 보여주고, 선택하지 않으면 전체를 보여줌

        List<InspectionResponseDto> response = inspectionService.getInspectionList(InspectionType.STORAGE, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 주문 검수 목록 조회
    @GetMapping("/inspections/order")
    public ResponseEntity<ApiResponse<List<InspectionResponseDto>>> getOrderInspections(
            @RequestParam(required = false) InspectionStatus status) {

        List<InspectionResponseDto> response = inspectionService.getInspectionList(InspectionType.ORDER, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 검수 상세 조회
     */
    @GetMapping("/inspections/{inspectionId}")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> getInspection(
            @PathVariable Long inspectionId) {

        InspectionResponseDto response = inspectionService.getInspection(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
