package com.project.rare_x_back.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 회원가입 관련
    EMAIL_DUPLICATED("EMAIL_DUPLICATED", "이미 가입된 이메일입니다"),
    PASSWORD_MISMATCH("PASSWORD_MISMATCH", "비밀번호가 일치하지 않습니다"),

    // 로그인 관련 추가
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    INVALID_PASSWORD("INVALID_PASSWORD", "비밀번호가 일치하지 않습니다"),
    ACCOUNT_NOT_ACTIVE("ACCOUNT_NOT_ACTIVE", "활성화되지 않은 계정입니다"),
    ACCOUNT_DELETED("ACCOUNT_DELETED", "탈퇴한 계정입니다"),

    // 사용자 관련
    USER_ALREADY_DELETED("USER_ALREADY_DELETED", "탈퇴한 회원입니다"),

    // 인증 관련
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN("EXPIRED_TOKEN", "만료된 토큰입니다"),

    // 이메일 관련
    EMAIL_SEND_FAILED("EMAIL_SEND_FAILED", "이메일 발송에 실패했습니다"),
    EMAIL_ALREADY_VERIFIED("EMAIL_ALREADY_VERIFIED", "이미 인증된 이메일입니다"),

    // 이메일 인증 추가 필요
    INVALID_VERIFICATION_CODE("INVALID_VERIFICATION_CODE", "인증번호가 일치하지 않습니다"),
    VERIFICATION_CODE_EXPIRED("VERIFICATION_CODE_EXPIRED", "인증번호가 만료되었습니다"),

    // 서버 에러
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
