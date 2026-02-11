package com.project.rare_x_back.dto.request;

import com.project.rare_x_back.enums.EmailType;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessageRequestDto {
    private String to;          /// 수신자
    private EmailType type;     /// 이메일 타입 (VERIFICATION, TEMP_PASSWORD)
}
