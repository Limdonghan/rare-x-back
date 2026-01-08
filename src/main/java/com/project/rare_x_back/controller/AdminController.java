package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.BrandListResponseDto;
import com.project.rare_x_back.dto.response.CategoryListResponseDto;
import com.project.rare_x_back.service.AdminService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

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

}
