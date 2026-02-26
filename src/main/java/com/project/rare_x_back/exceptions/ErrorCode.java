package com.project.rare_x_back.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 회원가입 관련
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),

    // 회원탈퇴 관련
    ACTIVE_ORDER_EXISTS(HttpStatus.BAD_REQUEST, "진행 중인 주문이 있어 탈퇴할 수 없습니다."),
    PENDING_SETTLEMENT_EXISTS(HttpStatus.BAD_REQUEST, "미정산 건이 있어 탈퇴할 수 없습니다."),
    UNPAID_PENALTY_EXISTS(HttpStatus.BAD_REQUEST, "미납 패널티가 있어 탈퇴할 수 없습니다."),
    ACTIVE_STORAGE_EXISTS(HttpStatus.BAD_REQUEST, "보관 중인 상품이 있어 탈퇴할 수 없습니다."),
    ACTIVE_STORAGE_REQUEST_EXISTS(HttpStatus.BAD_REQUEST, "진행 중인 보관 신청이 있어 탈퇴할 수 없습니다."),

    // 사용자 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),     // 미가입 유저
    USER_ALREADY_DELETED(HttpStatus.GONE, "사용자를 찾을 수 없습니다"),    // 탈퇴 유저(softDelete)
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "활성화되지 않은 계정입니다"),

    // 인증 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    // 이메일 관련
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다"),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증된 이메일입니다"),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다"),

    // 이메일 인증 추가 필요
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다"),

    // 상품 관련
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),

    // 관심 상품 관련
    WISH_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 관심 상품에 등록되어 있습니다."),
    WISH_NOT_FOUND(HttpStatus.NOT_FOUND, "관심 상품에 등록되어 있지 않습니다."),
    PRODUCT_DELETED(HttpStatus.BAD_REQUEST, "삭제된 상품입니다."),

    // 서버 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),

    // 검색 관련
    SEARCH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "검색 중 오류가 발생했습니다"),

    // 찾는 데이터가 없을 시 에러
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리소스가 존재하지 않습니다."),

    PASSWORDLESS_USER_CANNOT_LOGIN(HttpStatus.UNAUTHORIZED, "패스워드리스 로그인 유저입니다. 패스워드리스 방식으로 로그인 해주세요."),

    PASSWORDLESS_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "패스워드리스 서비스를 사용하고 있지 않습니다"),

    TEMP_PASSWORD_EXPIRED(HttpStatus.UNAUTHORIZED, "임시 비밀번호가 만료되었습니다. 다시 발급받아 주세요."),

    // 비밀번호 변경 관련
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."),
    CURRENT_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "현재 비밀번호를 입력해주세요"),

    // 잘못된 요청
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),

    // TossPayment 에러
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST,"결제 승인에 실패했습니다."),
    TOSS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "토스 페이먼츠 연동 중 오류가 발생했습니다."),
    BILLING_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 빌링키입니다."),
    BILLING_KEY_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "카드 등록이 필요합니다."),

    // Passwordless 에러
    PASSWORDLESS_ALREADY_IN_USE(HttpStatus.CONFLICT, "이미 패스워드리스 서비스를 사용 중입니다."),
    TEMPORARY_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "임시 인증 토큰이 없습니다."),
    TEMPORARY_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "임시 인증 토큰이 만료되었습니다."),

    // bid 에러
    PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST,"현재 판매중인 상품이 아닙니다."),
    PRODUCT_NOT_ON_BID(HttpStatus.BAD_REQUEST,"존재하지 않는 입찰입니다"),

    // 결제취소(환불)에러
    PAYMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "결제 정보를 찾을 수 없습니다."),
    ALREADY_CANCELED(HttpStatus.BAD_REQUEST,"이미 취소된 결제입니다."),
    INVALID_CANCEL_AMOUNT(HttpStatus.BAD_REQUEST,"유효하지 않은 취소 금액 입니다."),

    // 임시 비번 및 비번 변경
    TEMP_PASSWORD_ALREADY_SENT(HttpStatus.TOO_MANY_REQUESTS,"임시 비밀번호가 이미 발송되었습니다. 1시간 후 재시도해주세요"),

    // Order 에러
    ORDER_NOT_FOUND(HttpStatus.BAD_REQUEST,"현재 주문 정보를 찾을 수 없습니다");
    private final String message;
    private final HttpStatus status;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
