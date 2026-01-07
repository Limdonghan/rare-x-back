package com.project.rare_x_back.controller;

import com.project.rare_x_back.dto.request.BrandCreateRequestDto;
import com.project.rare_x_back.dto.request.CategoryCreateRequestDto;
import com.project.rare_x_back.dto.request.ProductCreateRequestDto;
import com.project.rare_x_back.dto.request.ProductUpdateRequestDto;
import com.project.rare_x_back.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    //상품 등록
    @PostMapping("/products")
    public ResponseEntity createProduct(@RequestBody ProductCreateRequestDto productCreateRequestDto) {
        // 서비스 호출 후 생성된 상품의 ID를 반환받음
        adminService.createProduct(productCreateRequestDto);
        // 성공 응답(200 OK 또는 201 Created)과 함께 생성된 ID를 반환
        return ResponseEntity.ok("상품 등록 완료");
    }

    //상품 정보 수정
    @PatchMapping("/products/{productId}")
    public ResponseEntity updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductUpdateRequestDto productUpdateRequestDto
    ) {
        adminService.updateProduct(productUpdateRequestDto, productId);
        return ResponseEntity.ok("상품 정보 수정 완료");
    }

    //카테고리 추가
    @PostMapping("/categories")
    public ResponseEntity createCategory(@RequestBody CategoryCreateRequestDto categoryCreateRequestDto){

        adminService.createCategory(categoryCreateRequestDto);
        return ResponseEntity.ok("카테고리 추가 완료");
    }

    //브랜드 추가
    @PostMapping("/brands")
    public ResponseEntity createBrand(@RequestBody BrandCreateRequestDto brandCreateRequestDto){
        adminService.createBrand(brandCreateRequestDto);
        return ResponseEntity.ok("브랜드 추가 완료");
    }
}
