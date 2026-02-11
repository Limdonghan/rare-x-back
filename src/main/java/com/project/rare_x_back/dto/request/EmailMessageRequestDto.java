package com.project.rare_x_back.dto.request;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessageRequestDto {
    private String to;          /// 수신자
    private String title;       /// 메일 제목
    private String body;        /// 메일 내용
}
