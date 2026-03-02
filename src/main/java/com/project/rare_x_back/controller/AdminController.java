package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.service.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final AdminUserService adminUserService;
    private final AdminDashboardService adminDashboardService;
    private final ProductDemandRequestService productDemandRequestService;

    private static final String ANONYMOUS_ADMIN = "anonymous";

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
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProduct(
            @RequestParam(required = false) Long categoryId,
            Pageable pageable){
        Page<ProductResponseDto> response = adminService.getAllProducts(categoryId, pageable);
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
            @Valid @RequestPart("data") ProductUpdateRequestDto productUpdateRequestDto,
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

    // 보관 주문 발송 처리 (PASSED → SHIPPED)
    // 보관 상품 주문은 Inspection이 없으므로 InspectionController 대신 이 API 사용
    @PatchMapping("/orders/{orderId}/ship")
    public ResponseEntity<ApiResponse<Void>> shipStorageOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        orderService.shipStorageOrder(orderId);
        log.info("관리자({})가 보관 주문 {}를 발송 처리함", adminDetails.getUsername(), orderId);
        return ResponseEntity.ok(ApiResponse.success("발송 처리되었습니다."));
    }

    // 주문 일괄 배송 완료 처리
    @PatchMapping("/orders/bulk/delivered")
    public ResponseEntity<ApiResponse<Void>> bulkDeliveredOrders(
            @RequestBody Map<String, List<Long>> request,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        if (request == null || !request.containsKey("orderIds")) {
            log.warn("관리자({})가 유효하지 않은 일괄 배송 완료 요청을 보냈습니다. request 또는 orderIds 키가 없습니다.",
                    adminDetails != null ? adminDetails.getUsername() : ANONYMOUS_ADMIN);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.success("유효한 주문 ID 목록(orderIds)이 요청에 포함되어야 합니다."));
        }

        List<Long> orderIds = request.get("orderIds");
        if (orderIds == null || orderIds.isEmpty()) {
            log.warn("관리자({})가 비어 있거나 null인 주문 ID 목록으로 일괄 배송 완료 요청을 보냈습니다.",
                    adminDetails != null ? adminDetails.getUsername() : ANONYMOUS_ADMIN);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.success("주문 ID 목록(orderIds)은 비어 있을 수 없습니다."));
        }

        orderService.bulkDeliveryComplete(orderIds);
        log.info("관리자({})가 주문 {}건을 일괄 배송 완료 처리함",
                adminDetails != null ? adminDetails.getUsername() : ANONYMOUS_ADMIN, orderIds.size());
        return ResponseEntity.ok(ApiResponse.success("일괄 배송 완료 처리되었습니다."));
    }

    // 카테고리 일괄 삭제
    @DeleteMapping("/categories/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteCategories(
            @RequestBody Map<String, List<Long>> request) {
        adminService.bulkDeleteCategories(request.get("ids"));
        return ResponseEntity.ok(ApiResponse.success("카테고리 일괄 삭제가 완료되었습니다."));
    }

    // 브랜드 일괄 삭제
    @DeleteMapping("/brands/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteBrands(
            @RequestBody Map<String, List<Long>> request) {
        adminService.bulkDeleteBrands(request.get("ids"));
        return ResponseEntity.ok(ApiResponse.success("브랜드 일괄 삭제가 완료되었습니다."));
    }

    // ====== 관리자 회원 관리 (MANAGER-004) ======

    // 회원 목록 조회 + 통계
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserListResponseDto>> getAdminUsers(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String providerType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        AdminUserListResponseDto result = adminUserService.getAdminUsers(status, providerType, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 회원 상세 조회
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponseDto>> getAdminUserDetail(
            @PathVariable Long userId) {

        AdminUserDetailResponseDto result = adminUserService.getAdminUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 회원 상태 변경
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusRequestDto request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {

        adminUserService.updateUserStatus(userId, request);
        log.info("관리자({})가 회원 {} 상태를 {} 으로 변경", adminDetails.getUsername(), userId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("회원 상태가 변경되었습니다."));
    }

    // 회원 일괄 상태 변경
    @PatchMapping("/users/bulk/status")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateUserStatus(
            @Valid @RequestBody AdminUserStatusRequestDto request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {

        adminUserService.bulkUpdateUserStatus(request);
        log.info("관리자({})가 회원 {}명 상태를 {} 으로 일괄 변경",
                adminDetails.getUsername(), request.getUserIds().size(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("회원 상태가 일괄 변경되었습니다."));
    }

    // 관리자 대시보드 통계
    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<AdminDashboardResponseDto>> getDashboardSummary(
            @RequestParam(required = false) String date
    ) {
        AdminDashboardResponseDto result = adminDashboardService.getSummary(date);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/dashboard/revenue/daily")
    public ResponseEntity<ApiResponse<List<AdminDailyRevenueResponseDto>>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminDashboardService.getDailyRevenue(startDate, endDate)
        ));
    }


    // 유저 상품 등록 요청 목록 조회
    @GetMapping("/products/demand-requests")
    public ResponseEntity<ApiResponse<ProductDemandPageResponseDto<ProductDemandRequestResponseDto>>> getList(
            @RequestParam(required = false) Integer period,
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        productDemandRequestService
                                .getDemandRequests(period, pageable)
                )
        );
    }

    // 유저 상품 등록 요청 상세 조회
    @GetMapping("/products/demand-requests/{demandId}")
    public ResponseEntity<ApiResponse<ProductDemandRequestDetailResponseDto>> getDetail(@PathVariable Long demandId) {
        ProductDemandRequestDetailResponseDto responseDto = productDemandRequestService.getDemandRequestDetail(demandId);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    // 유저 상품 등록 요청 삭제
    @DeleteMapping("/products/demand-requests/{demandId}")
    public ResponseEntity<ApiResponse<Void>> deleteRequest (@PathVariable Long demandId) {
        productDemandRequestService.deleteDemandRequest(demandId);
        return ResponseEntity.ok(ApiResponse.success("상품 등록 요청 삭제가 완료되었습니다."));
    }


}