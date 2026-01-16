package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.BrandListResponseDto;
import com.project.rare_x_back.dto.response.CategoryListResponseDto;
import com.project.rare_x_back.dto.response.ProductListResponseDto;
import com.project.rare_x_back.entity.Brand;
import com.project.rare_x_back.entity.Category;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.ProductImage;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.BrandRepository;
import com.project.rare_x_back.repository.CategoryRepository;
import com.project.rare_x_back.repository.ProductImageRepository;
import com.project.rare_x_back.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final S3ImageService s3ImageService;
    private final ProductImageRepository productImageRepository;

    //상품 등록
    public void createProduct(ProductCreateRequestDto productCreateRequestDto) {

        Brand brand = brandRepository.findById(productCreateRequestDto.getBrandId())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "존재하지 않는 브랜드 입니다."
                        ));

        Category category = categoryRepository.findById(productCreateRequestDto.getCategoryId())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "존재하지 않는 카테고리 입니다."
                        ));

        Product product = Product.builder()
                .productName(productCreateRequestDto.getProductName())
                .productDescription(productCreateRequestDto.getProductDescription())
                .retailPrice(productCreateRequestDto.getRetailPrice())
                .brand(brand)
                .category(category)
                .build();

        productRepository.save(product);

    }

    //상품 이미지 등록 (DB저장 실패 시 S3 롤백 로직 추가)
    public List<String> saveProductImage(Long productId, List<MultipartFile> images) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "상품을 찾을 수 없습니다."
                ));
        //저장된 url 담을 빈그릇 생성
        List<String> uploadUrls = new ArrayList<>(); //s3업로드 성공
        List<String> saveUrls = new ArrayList<>(); //

        try {
            for (MultipartFile file : images) { //s3 업로드 + DB저장
                if (file == null || file.isEmpty()) continue;

                //s3 업로드
                String imageUrl = s3ImageService.uploadProductImage(file);
                uploadUrls.add(imageUrl);
                //DB 저장
                ProductImage productImage = ProductImage.builder()
                        .imageUrl(imageUrl)
                        .product(product)
                        .build();
                productImageRepository.save(productImage);
                saveUrls.add(imageUrl);
            }
            return saveUrls;

        } catch (Exception e) {
            //실패 시 s3 롤백
            log.error("상품 이미지 저장 실패. S3 롤백 시작", e);

            for (String url : uploadUrls) {
                try {
                    s3ImageService.deleteImageByUrl(url);
                } catch (Exception deleteEx) {
                    log.error("S3 이미지 삭제 실패: {}", url, deleteEx);
                }
            }
            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "상품 이미지 저장 중 오류가 발생했습니다.");
        }
    }

    //상품 조회(전체 조회(목록)이니까 이미지는 여러개 있어도 썸네일 이미지만 가져옴.)
    public Page<ProductListResponseDto> getAllProducts(Pageable pageable) {
        // 1. @EntityGraph가 있는 findAll(pageable) 실행해서 전체 조회
        Page<Product> productPage = productRepository.findAllByIsDeletedFalse(pageable);
        // 2. map -> 리스트나 페이지안에 들어있는 내용물들을 하나씩 꺼내서 내가 원하는 다른 DTO로 바꾸고 다시 집어넣음
        return productPage.map(product -> {
            // 썸네일 이미지 URL 추출(없으면 null,썸네일 이미지는 상품 하나에 연결된 모든 이미지 리스트 중 0번 인덱스)
            String firstImageUrl = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                firstImageUrl = product.getImages().get(0).getImageUrl();
            }
            // DTO에 담음
            return ProductListResponseDto.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .productDescription(product.getProductDescription())
                    .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : "등록된 브랜드가 없습니다.")
                    .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : "등록된 카테고리가 없습니다.")
                    .imageUrl(firstImageUrl)
                    .build();
        });
    }

    //상품 상세 조회
    public ProductListResponseDto getDetailProduct(Long productId){
        Product product = productRepository.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(()->
                        new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "상품을 찾을 수 없습니다."
                        ));

        // 이미지 객체 리스트를 URL만 있는 문자열 리스트로 변환
        List<String> imageUrls = product.getImages().stream()
                .map(ProductImage :: getImageUrl)
                .toList();

        return ProductListResponseDto.builder()
                .productId(productId)
                .productName(product.getProductName())
                .productDescription(product.getProductDescription())
                .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : "등록된 브랜드가 없습니다.")
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : "등록된 카테고리가 없습니다.")
                .imageUrls(imageUrls)
                .build();
    }

    //상품 정보 수정
    public void updateProduct(ProductUpdateRequestDto productUpdateRequestDto, Long productId,
                              List<Long> deleteImageIds, List<MultipartFile> newFiles
    ){
        //수정할 상품 존재여부 확인
        Product product = productRepository.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(()->
                        new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "상품을 찾을 수 없습니다."
                        ));
        //수정된 값만 json채워 보내주고, 수정안된 값은 null로 채움
        Brand brand = null;
        if (productUpdateRequestDto.getBrandId() != null) {
            brand = brandRepository.findById(productUpdateRequestDto.getBrandId())
                    .orElseThrow(() -> new CustomException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "존재하지 않는 브랜드입니다."
                    ));
        }

        Category category = null;
        if (productUpdateRequestDto.getCategoryId() != null) {
            category = categoryRepository.findById(productUpdateRequestDto.getCategoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND,
                            "존재하지 않는 카테고리 입니다."
                    ));
        }
        //엔티티 업데이트
        product.updateProductInfo(
                productUpdateRequestDto.getProductName(),
                productUpdateRequestDto.getProductDescription(),
                productUpdateRequestDto.getRetailPrice(),
                brand,
                category
        );
        //이미지 선택 삭제 (deleteImageIds 있을 때만)
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            for (Long id : deleteImageIds) {      //DB에서 이미지 정보 조회
                ProductImage productImage = productImageRepository.findById(id)
                        .orElseThrow(() -> new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND, "삭제할 이미지를 찾을 수 없습니다."));
                //s3에서 실제 파일 삭제
                s3ImageService.deleteImageByUrl(productImage.getImageUrl());
                //product의 리스트에서 삭제 (DB row 삭제)
                product.getImages().remove(productImage);
            }
        }
        //새 이미지 추가 (newFiles가 있을 때만)
        if (newFiles != null && !newFiles.isEmpty()) {
            for (MultipartFile file : newFiles) {
                if (!file.isEmpty()) {
                    //s3에 업로드
                    String url = s3ImageService.uploadProductImage(file);
                    //DB 저장 및 연관관계 설정
                    ProductImage newProdImg = ProductImage.builder()
                            .imageUrl(url)
                            .product(product)
                            .build();
                    productImageRepository.save(newProdImg);
                }
            }
        }
    }

    //상품 삭제 (연결된 s3이미지도 같이 삭제 추가)
    public void deleteProduct (Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "상품을 찾을 수 없습니다."
                        ));
        //s3에서 실제 파일 삭제 (반드시 s3이미지 부터 지워야 함)
        if(product.getImages() != null) {
            for (ProductImage productImage : product.getImages()) {
                s3ImageService.deleteImageByUrl(productImage.getImageUrl());
            }
            // 연관관계 컬렉션을 비워 orphanRemoval 을 트리거하여 이미지 엔티티를 DB에서 물리적으로 삭제 (상품은 논리 삭제)
            product.getImages().clear();
        }

        product.updateIsDeleted(true);
    }

    //카테고리 조회
    public List<CategoryListResponseDto> getAllCategory () {
        //카테고리 전체 조회
        List<Category> results = categoryRepository.findAll();
        List<CategoryListResponseDto> response = new ArrayList<>();

        for(Category category : results){
            CategoryListResponseDto newResult = new CategoryListResponseDto(
                    category.getCategoryName()
            );
            response.add(newResult);
        }
        return response;
    }

    //카테고리 등록
    public void createCategory (CategoryCreateRequestDto categoryCreateRequestDto) {

        Category category = Category.builder()
                .categoryName(categoryCreateRequestDto.getCategoryName())
                .build();

        categoryRepository.save(category);
    }

    //카테고리 수정
    public void updateCategory (CategoryUpdateRequestDto categoryUpdateRequestDto, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "카테고리를 찾을 수 없습니다."
                ));
        category.updateCategoryInfo(categoryUpdateRequestDto.getCategoryName());
    }

    //카테고리 삭제
    public void deleteCategory (Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()->
                        new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "카테고리를 찾을 수 없습니다.")
                );
        categoryRepository.deleteById(category.getCategoryId());
    }

    //브랜드 조회
    public List<BrandListResponseDto> getAllBrand (Pageable pageable) {
        //브랜드 전체 조회
        List<Brand> results = brandRepository.findBrandsByIsDeletedFalse(pageable);
        List<BrandListResponseDto> response = new ArrayList<>();

        for(Brand brand : results){
            BrandListResponseDto newResult = new BrandListResponseDto(
                    brand.getBrandName()
            );
            response.add(newResult);
        }
        return response;
    }

    //브랜드 등록
    public void createBrand (BrandCreateRequestDto brandCreateRequestDto) {

        Brand brand = Brand.builder()
                .brandName(brandCreateRequestDto.getBrandName())
                .build();
        brandRepository.save(brand);
    }

    //브랜드 수정
    public void updateBrand (BrandUpdateRequestDto brandUpdateRequestDto, Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "브랜드를 찾을 수 없습니다."
                ));
        brand.updateBrandInfo(brandUpdateRequestDto.getBrandName());
    }

    //브랜드 삭제
    public void deleteBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "브랜드를 찾을 수 없습니다."
                ));
        brandRepository.deleteById(brand.getBrandId());
    }

}
