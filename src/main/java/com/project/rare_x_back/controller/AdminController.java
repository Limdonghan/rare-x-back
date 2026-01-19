package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.dto.response.BrandListResponseDto;
import com.project.rare_x_back.dto.response.CategoryListResponseDto;
import com.project.rare_x_back.dto.response.InspectionResponseDto;
import com.project.rare_x_back.dto.response.ProductListResponseDto;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.service.AdminService;
import com.project.rare_x_back.service.InspectionService;
import com.project.rare_x_back.service.S3ImageService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final S3ImageService s3ImageService;
    private final InspectionService inspectionService;

    //s3 이미지 업로드
    @PostMapping("/products/{productId}/images")
    public ResponseEntity<ApiResponse<List<String>>> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("images")List<MultipartFile> images
            ) {
        List<String> imageUrls = adminService.saveProductImage(productId, images);
        return ResponseEntity.ok(ApiResponse.success(imageUrls,"상품 이미지 등록이 완료되었습니다."));
    }

    //상품 등록
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Void>> createProduct(
            @Valid @RequestBody ProductCreateRequestDto productCreateRequestDto,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        // 서비스 호출 후 생성된 상품의 ID를 반환받음
        adminService.createProduct(productCreateRequestDto);
        log.info("상품 {}이 관리자 ID {}에 의해 등록됨", productCreateRequestDto.getProductName(), adminDetails.getUsername());
        // 성공 응답(201 Created)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("상품 등록이 완료되었습니다."));
    }

    //상품 전체 조회
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ProductListResponseDto>>> getAllProduct(Pageable pageable){
        Page<ProductListResponseDto> response = adminService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //상품 상세 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductListResponseDto>> getDetailProduct(@PathVariable Long productId) {
        ProductListResponseDto response = adminService.getDetailProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //상품 정보 수정
    @PatchMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestPart("data")  ProductUpdateRequestDto productUpdateRequestDto,
            @RequestParam(value = "deleteIds", required = false) List<Long> deleteIds,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        adminService.updateProduct(productUpdateRequestDto, productId, deleteIds, newImages);
        log.info("상품 ID {}이 관리자 ID {}에 의해 수정됨", productId, adminDetails.getUsername()); //수정 기록 로그
        return ResponseEntity.ok(ApiResponse.success("상품 정보 수정이 완료되었습니다."));
    }

    //상품 삭제
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct (
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        adminService.deleteProduct(productId);
        log.info("상품 ID {}이 관리자 ID {}에 의해 삭제됨", productId, adminDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("상품 삭제가 완료되었습니다."));
    }

    //카테고리 조회
    @GetMapping("/categories")
    public ResponseEntity <ApiResponse<List<CategoryListResponseDto>>> getAllCategory(){
        List<CategoryListResponseDto> results = adminService.getAllCategory();
        return ResponseEntity.ok(ApiResponse.success(results));
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

    //브랜드 조회
    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<BrandListResponseDto>>> getAllBrands (Pageable pageable) {
        List<BrandListResponseDto> results = adminService.getAllBrand(pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    //브랜드 추가
    @PostMapping("/brands")
    public ResponseEntity<ApiResponse<Void>> createBrand(@Valid @RequestBody BrandCreateRequestDto brandCreateRequestDto){
        adminService.createBrand(brandCreateRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("브랜드 등록이 완료되었습니다."));
    }

    //브랜드 수정
    @PatchMapping("/brands/{brandId}")
    public ResponseEntity <ApiResponse<Void>> updateBrand(
            @PathVariable Long brandId,
            @Valid @RequestBody BrandUpdateRequestDto brandUpdateRequestDto) {
        adminService.updateBrand(brandUpdateRequestDto, brandId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("브랜드 정보 수정이 완료되었습니다."));
    }

    //브랜드 삭제
    @DeleteMapping("/brands/{brandId}")
    public ResponseEntity <ApiResponse<Void>> deleteBrand(@PathVariable Long brandId) {
        adminService.deleteBrand(brandId);
        return ResponseEntity.ok(ApiResponse.success("브랜드 삭제가 완료되었습니다."));
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

    /**
     * 도착 확인 (SHIPPED_TO_WAREHOUSE → PENDING_INSPECTION)
     */
    @PatchMapping("/inspections/{inspectionId}/confirm-arrival")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> confirmArrival(
            @PathVariable Long inspectionId) {

        InspectionResponseDto response = inspectionService.confirmArrival(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response, "도착 확인이 완료되었습니다"));
    }

    /**
     * 검수 시작 (PENDING_INSPECTION → INSPECTING)
     * - 담당자 배정
     * - 체크리스트 생성
     * - 시작 시간 기록
     */
    @PatchMapping("/inspections/{inspectionId}/start")
    public ResponseEntity<ApiResponse<InspectionResponseDto>> startInspection(
            @PathVariable Long inspectionId,
            @AuthenticationPrincipal Long adminId) {

        InspectionResponseDto response = inspectionService.startInspection(inspectionId, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "검수가 시작되었습니다"));
    }

    /**
     * 체크리스트 조회
     */
    @GetMapping("/inspections/{inspectionId}/checklist")
    public ResponseEntity<ApiResponse<InspectionChecklistResponseDto>> getChecklist(
            @PathVariable Long inspectionId) {

        InspectionChecklistResponseDto response = inspectionService.getChecklist(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(response, "체크리스트 조회 성공"));
    }

    /**
     * 체크리스트 수정
     */
    @PatchMapping("/inspections/{inspectionId}/checklist")
    public ResponseEntity<ApiResponse<InspectionChecklistResponseDto>> updateChecklist(
            @PathVariable Long inspectionId,
            @RequestBody InspectionChecklistRequestDto request) {

        InspectionChecklistResponseDto response = inspectionService.updateChecklist(inspectionId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "체크리스트 수정 성공"));
    }
}
