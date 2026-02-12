package com.project.rare_x_back.dto.request;

import com.project.rare_x_back.enums.EmailType;
import lombok.*;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessageRequestDto {
    private String to;          /// 수신자
    private EmailType type;     /// 이메일 타입 (VERIFICATION, TEMP_PASSWORD)
    private String content;     /// 암호화된 내용 (인증코드 또는 임시비밀번호)
}
