package com.project.rare_x_back.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {       // 전역 API에서 사용
    // 필드: 성공 여부, 메시지, 데이터, 에러
    private boolean success;
    private T data;  // data는 어떤 타입이든 가능
    private String message;
    private ErrorResponse error;

    // 성공 응답 (데이터 + 메시지)
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    // 성공 응답 (데이터만)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    // 성공 응답 (메시지만)
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, null, new ErrorResponse(code, message));
    }

    // 내부 클래스
    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}
