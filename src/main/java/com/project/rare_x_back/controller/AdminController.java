package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.BrandListResponseDto;
import com.project.rare_x_back.dto.response.CategoryListResponseDto;
import com.project.rare_x_back.dto.response.ProductResponseDto;
import com.project.rare_x_back.repository.ProductRepository;
import com.project.rare_x_back.service.AdminService;
import com.project.rare_x_back.service.OrderService;
import com.project.rare_x_back.service.S3ImageService;
import com.project.rare_x_back.service.SearchService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final OrderService orderService;
    private final SearchService searchService;
    private final ProductRepository productRepository;

    //s3 이미지 업로드
    @PostMapping(value = "/products/{productId}/images",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadProductImage(
            @PathVariable Long productId,
            @RequestPart("images")List<MultipartFile> images
            ) {
        List<String> imageUrls = adminService.saveProductImage(productId, images);
        return ResponseEntity.ok(ApiResponse.success(imageUrls,"상품 이미지 등록이 완료되었습니다."));
    }

    //상품 등록
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Long>> createProduct(
            @Valid @RequestBody ProductCreateRequestDto productCreateRequestDto,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        // 서비스 호출 후 생성된 상품의 ID를 반환받음
        Long savedProductId = adminService.createProduct(productCreateRequestDto);
        log.info("상품 {}이 관리자 ID {}에 의해 등록됨", productCreateRequestDto.getProductName(), adminDetails.getUsername());
        // 성공 응답(201 Created)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(savedProductId,"상품 등록이 완료되었습니다."));
    }

    //상품 전체 조회
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProduct(Pageable pageable){
        Page<ProductResponseDto> response = adminService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //상품 상세 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getDetailProduct(@PathVariable Long productId) {
        ProductResponseDto response = adminService.getDetailProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //상품 정보 수정
    @PatchMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestPart("data")  ProductUpdateRequestDto productUpdateRequestDto,
            @RequestParam(value = "deleteIds", required = false) List<String> deleteIds,
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

    // 주문 조회

    // 주문 상세 조회

    // 주문 배송 완료로 상태 변경
    @PatchMapping("/orders/{orderId}/delivered")
    public ResponseEntity <ApiResponse<Void>> deliveredOrder (
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails adminDetails
            ) {
        orderService.deliveryComplete(orderId);
        log.info("관리자({})가 주문 {}를 배송 완료 처리함", adminDetails.getUsername(), orderId);
        return ResponseEntity.ok(ApiResponse.success("배송 완료 처리되었습니다."));
    }


}
