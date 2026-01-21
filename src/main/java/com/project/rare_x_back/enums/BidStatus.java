package com.project.rare_x_back.enums;

///  입찰 상태
public enum BidStatus {
    OPEN,           /// 등록 및 체결중
    MATCHED,        /// 체결 완료
    CANCELED,       /// 취소
    EXPIRED         /// 입찰 기간 만료
}
