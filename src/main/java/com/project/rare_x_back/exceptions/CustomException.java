package com.project.rare_x_back.exceptions;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    // 생성자 1 -> 메시지를 따로 안 주면 ErrorCode에 정의된 기본 메시지 사용
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 생성자 2 -> 메시지 오버라이드 (에러 종류는 같은데, 메시지만 상황별로 다를 때)
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
