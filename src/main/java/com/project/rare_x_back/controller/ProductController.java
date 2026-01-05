package com.project.rare_x_back.controller;

import com.project.rare_x_back.dto.request.CategoryCreateRequestDto;
import com.project.rare_x_back.dto.request.ProductCreateRequestDto;
import com.project.rare_x_back.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class ProductController {

    private final ProductService productService;

    //상품 등록
    @PostMapping("/products")
    public ResponseEntity createProduct(@RequestBody ProductCreateRequestDto productCreateRequestDto) {
        // 서비스 호출 후 생성된 상품의 ID를 반환받음
        productService.createProduct(productCreateRequestDto);
        // 성공 응답(200 OK 또는 201 Created)과 함께 생성된 ID를 반환
        return ResponseEntity.ok("상품 등록 완료");
    }

    //카테고리 추가
    @PostMapping("/categories")
    public ResponseEntity createCategory(@RequestBody CategoryCreateRequestDto categoryCreateRequestDto){

        productService.createCategory(categoryCreateRequestDto);
        return ResponseEntity.ok("카테고리 추가 완료");
    }
}
