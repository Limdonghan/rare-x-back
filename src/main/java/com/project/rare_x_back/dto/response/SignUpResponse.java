package com.project.rare_x_back.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpResponse {
    private String email;
    private String name;
    private String message;
}
