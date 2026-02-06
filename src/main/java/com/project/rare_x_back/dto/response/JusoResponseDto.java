package com.project.rare_x_back.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JusoResponseDto {

    private Results results;

    @Getter
    @Setter
    public static class Results {
        private CommonDto common;
        private List<JusoDto> juso;
    }

    @Getter
    @Setter
    public static class CommonDto {
        private String errorMessage;
        private String errorCode;
        private String totalCount;
    }

    @Getter
    @Setter
    public static class JusoDto {
        private String roadAddr; // 전체 도로명 주소
        private String zipNo;   // 우편번호
        private String bdNm;    // 건물명
    }


}
