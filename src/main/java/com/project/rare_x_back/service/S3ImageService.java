package com.project.rare_x_back.service;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

@RequiredArgsConstructor
@Component
@Service
public class S3ImageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucket;

    @Value("${cloud.aws.s3.region}")
    private String region;

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




}
