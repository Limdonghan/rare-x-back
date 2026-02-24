package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponseDto {

    // 통계
    private long totalCount;
    private long activeCount;
    private long bannedCount;
    private long blockedCount;
    private long quitedCount;

    // 유저 목록
    private Page<UserItem> users;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserItem {
        private Long userId;
        private String userNumber;      // USR-00001
        private String name;
        private String email;
        private String providerType;    // LOCAL, GOOGLE
        private LocalDateTime createdAt;
        private String status;          // ACTIVE, BANNED, BLOCKED, QUITED
    }
}