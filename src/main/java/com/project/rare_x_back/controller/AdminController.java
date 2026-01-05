package com.project.rare_x_back.controller;

import com.project.rare_x_back.dto.ProductCreateRequestDto;
import com.project.rare_x_back.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/products")
    public ResponseEntity<Long> createProduct(@RequestBody ProductCreateRequestDto productCreateRequestDto) {
        // 서비스 호출 후 생성된 상품의 ID를 반환받음
        long productId = adminService.createProduct(productCreateRequestDto);
        // 성공 응답(200 OK 또는 201 Created)과 함께 생성된 ID를 반환
        return ResponseEntity.ok(productId);
    }
}
