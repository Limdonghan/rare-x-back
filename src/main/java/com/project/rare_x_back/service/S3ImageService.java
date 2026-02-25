package com.project.rare_x_back.service;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3ImageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucket;

    @Value("${cloud.aws.s3.region.static}")
    private String region;
    // 관리자 상품 등록 이미지 메소드
    public String uploadProductImage(MultipartFile file) {
        try {
            validateImage(file);

            String key = generateKey(file.getOriginalFilename()); //s3에 저장될 파일 경로

            PutObjectRequest putreq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putreq, RequestBody.fromBytes(file.getBytes()));
            //공개 url 반환
            return buildPublicUrl(key);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 업로드 중 서버 오류가 발생했습니다.");
        }
    }

    //이미지 검증 (이미지 파일이 맞는지, 용량과 크기 조건에 맞는지 검증함.)
    private void validateImage (MultipartFile file) {
        //파일이 있는지 검증
        if (file == null || file.isEmpty()) {
            throw new CustomException(
                    ErrorCode.BAD_REQUEST,
                    "이미지 파일이 비어있습니다."
            );
        }
        //파일이 이미지 파일인지 확인.
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(
                    ErrorCode.BAD_REQUEST,
                    "올바른 이미지 파일 형식이 아닙니다."
            );
        }
        //이미지 용량 체크
        long maxSize = 5 * 1024 * 1024; //5MB
        if(file.getSize() > maxSize) {
            throw new CustomException(
                    ErrorCode.BAD_REQUEST,
                    "이미지 용량 제한이 초과되었습니다.(5MB 이하)"
            );
        }
    }

    //업로드된 파일의 중복을 방지, 고유한 저장 경로 생성
    private String generateKey(String originalFilename){
        String ext = "";
        if(originalFilename.contains(".")){
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "products/" + UUID.randomUUID() + ext;
    }

    //key url 인코딩 -> key에 공백, 한글 같은 게 있으면 URL이 깨질 수 있어서
    // s3의 가상 호스팅 방식 url 형태로 변환
    private String buildPublicUrl(String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + encodedKey;
    }

    //Url에서 key 잘라냄
    private String extractKeyFromUrl (String url) {
        //url에서 문자열의 시작 위치 찾음. (찾는 문자열이 없으면 -1이 됨.)
        String keyword = ".amazonaws.com/";
        int idx = url.indexOf(keyword);

        if (idx == -1) {
            throw new CustomException(
                    ErrorCode.BAD_REQUEST, "S3 URL 형식이 아닙니다.");
        }
        //url에서 키 부분만 잘라옴.
        String encodedKey = url.substring(idx + keyword.length());
        return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
    }


    //잘라낸 key만 뽑아서 s3에서 삭제
    public void deleteImageByUrl (String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);

        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(delReq);
    }

    // ====================== 유저 상품 등록 요청 이미지

    // 상품 등록 요청 이미지
    public String uploadProductRequestImage(MultipartFile file) {
        try {
            validateImage(file);

            String key = requestProdGenerateKey(file.getOriginalFilename()); //s3에 저장될 파일 경로

            PutObjectRequest putreq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putreq, RequestBody.fromBytes(file.getBytes()));
            //공개 url 반환
            return buildPublicUrl(key);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 업로드 중 서버 오류가 발생했습니다.");
        }
    }

    //업로드된 파일의 중복을 방지, 고유한 저장 경로 생성
    private String requestProdGenerateKey(String originalFilename){
        String ext = "";
        if(originalFilename.contains(".")){
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "product_request/" + UUID.randomUUID() + ext;
    }



    // S3ImageService에 벌크 삭제
    public void deleteImageByUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        // url에서 s3 Object Key (파일명)만 추출
        List<ObjectIdentifier> keysToDelete = imageUrls.stream()
                .map(url -> ObjectIdentifier.builder().key(extractKeyFromUrl(url)).build())
                .collect(Collectors.toList());
        // 삭제 요청 구성
        Delete deleteRequest = Delete.builder()
                .objects(keysToDelete)
                .quiet(false)
                .build();

        DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(deleteRequest)
                .build();

        // s3에 벌크 삭제 요청 전송
        try {
            s3Client.deleteObjects(multiObjectDeleteRequest);
            log.info("{}개의 이미지 S3 삭제 완료", keysToDelete.size());
        } catch (S3Exception e) {
            log.error("S3 벌크 삭제 중 오류 발생: {}", e.awsErrorDetails().errorMessage());
        }
    }

}
