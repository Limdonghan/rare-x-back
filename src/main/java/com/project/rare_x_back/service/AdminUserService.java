package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.AdminUserStatusRequestDto;
import com.project.rare_x_back.dto.response.AdminUserDetailResponseDto;
import com.project.rare_x_back.dto.response.AdminUserListResponseDto;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.enums.ProviderType;
import com.project.rare_x_back.enums.Status;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BuyBidRepository buyBidRepository;
    private final SaleBidRepository saleBidRepository;
    private final SearchService searchService;

    // ====== 회원 목록 조회 + 통계 ======
    @Transactional(readOnly = true)
    public AdminUserListResponseDto getAdminUsers(
            List<String> status, String providerType, Pageable pageable) {

        // 1. 상태 필터 변환
        List<Status> statuses = null;
        if (status != null && !status.isEmpty()) {
            statuses = status.stream()
                    .map(s -> {
                        try {
                            return Status.valueOf(s);
                        } catch (IllegalArgumentException e) {
                            throw new CustomException(ErrorCode.BAD_REQUEST, "유효하지 않은 상태값입니다: " + s);
                        }
                    })
                    .toList();
        }

        // 2. 가입유형 필터 변환
        ProviderType provider = null;
        if (providerType != null && !providerType.isBlank()) {
            try {
                provider = ProviderType.valueOf(providerType);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.BAD_REQUEST, "유효하지 않은 가입유형입니다: " + providerType);
            }
        }

        // 3. 필터 조합별 조회
        Page<User> users;

        if (statuses != null && provider != null) {
            users = userRepository.findByStatusInAndProviderType(statuses, provider, pageable);
        } else if (statuses != null) {
            users = userRepository.findByStatusIn(statuses, pageable);
        } else if (provider != null) {
            users = userRepository.findByProviderType(provider, pageable);
        } else {
            users = userRepository.findAllForAdmin(pageable);
        }

        // 4. 통계 조회
        Map<Status, Long> statsMap = getStatsMap();

        // 5. 응답 조합
        Page<AdminUserListResponseDto.UserItem> userItems = users.map(this::toUserItem);

        return AdminUserListResponseDto.builder()
                .totalCount(statsMap.values().stream().mapToLong(Long::longValue).sum())
                .activeCount(statsMap.getOrDefault(Status.ACTIVE, 0L))
                .bannedCount(statsMap.getOrDefault(Status.BANNED, 0L))
                .blockedCount(statsMap.getOrDefault(Status.BLOCKED, 0L))
                .quitedCount(statsMap.getOrDefault(Status.QUITED, 0L))
                .users(userItems)
                .build();
    }

    // ====== 회원 상세 조회 ======
    @Transactional(readOnly = true)
    public AdminUserDetailResponseDto getAdminUserDetail(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 거래 건수
        long buyOrderCount = orderRepository.countByBuyer_UserId(userId);
        long sellOrderCount = orderRepository.countBySeller_UserId(userId);

        // 활성 입찰 건수
        long activeBuyBidCount = buyBidRepository.countByUser_UserIdAndStatus(userId, BidStatus.OPEN);
        long activeSellBidCount = saleBidRepository.countByUser_UserIdAndStatus(userId, BidStatus.OPEN);

        return AdminUserDetailResponseDto.builder()
                .userId(user.getUserId())
                .userNumber(String.format("USR-%05d", user.getUserId()))
                .name(user.getName())
                .email(user.getEmail())
                .providerType(user.getProviderType().name())
                .status(user.getStatus().name())
                .profileUrl(user.getProfileUrl())
                .createdAt(user.getCreatedAt())
                .buyOrderCount(buyOrderCount)
                .sellOrderCount(sellOrderCount)
                .activeBuyBidCount(activeBuyBidCount)
                .activeSellBidCount(activeSellBidCount)
                .build();
    }

    // ====== 회원 상태 변경 ======
    @Transactional
    public void updateUserStatus(Long userId, AdminUserStatusRequestDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 변경할 상태 검증
        Status newStatus;
        try {
            newStatus = Status.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "유효하지 않은 상태값입니다: " + request.getStatus());
        }

        // QUITED 회원은 상태 변경 불가
        if (user.getStatus() == Status.QUITED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "탈퇴한 회원은 상태를 변경할 수 없습니다.");
        }

        // QUITED로 변경 불가 (QUITED = 자진탈퇴)
        if (newStatus == Status.QUITED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "회원을 탈퇴 상태로 변경할 수 없습니다.");
        }

        // 현재 상태와 동일하면 변경 불가
        if (user.getStatus() == newStatus) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "현재 상태와 동일한 상태로 변경할 수 없습니다.");
        }

        Status oldStatus = user.getStatus();
        user.setStatus(newStatus);
        userRepository.save(user);

        // Typesense 인덱스 업데이트
        searchService.indexUser(user);

        log.info("회원 상태 변경: userId={}, {} -> {}", userId, oldStatus, newStatus);
    }

    // ====== 회원 일괄 상태 변경 ======
    @Transactional
    public void bulkUpdateUserStatus(AdminUserStatusRequestDto request) {

        // 1. userIds 검증
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "변경할 회원을 선택해주세요.");
        }

        // 2. 상태값 검증
        Status newStatus;
        try {
            newStatus = Status.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "유효하지 않은 상태값입니다: " + request.getStatus());
        }

        if (newStatus == Status.QUITED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "회원을 탈퇴 상태로 변경할 수 없습니다.");
        }

        // 3. 유저 목록 조회
        List<User> users = userRepository.findAllById(request.getUserIds());

        if (users.size() != request.getUserIds().size()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 회원이 포함되어 있습니다.");
        }

        // 4. QUITED 회원 포함 여부 검증
        boolean hasQuitedUser = users.stream()
                .anyMatch(user -> user.getStatus() == Status.QUITED);
        if (hasQuitedUser) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "탈퇴한 회원이 포함되어 변경할 수 없습니다.");
        }

        // 5. 상태 변경 + TypeSense 인덱스 업데이트
        for (User user : users) {
            Status oldStatus = user.getStatus();
            if (oldStatus != newStatus) {
                user.setStatus(newStatus);
                userRepository.save(user);
                searchService.indexUser(user);
                log.info("회원 상태 일괄변경: userId={}, {} -> {}", user.getUserId(), oldStatus, newStatus);
            }
        }
    }

    // ====== Private 메서드 ======

    private AdminUserListResponseDto.UserItem toUserItem(User user) {
        return AdminUserListResponseDto.UserItem.builder()
                .userId(user.getUserId())
                .userNumber(String.format("USR-%05d", user.getUserId()))
                .name(user.getName())
                .email(user.getEmail())
                .providerType(user.getProviderType().name())
                .createdAt(user.getCreatedAt())
                .status(user.getStatus().name())
                .build();
    }

    private Map<Status, Long> getStatsMap() {
        List<Object[]> results = userRepository.countByStatus();
        Map<Status, Long> map = new HashMap<>();
        for (Object[] row : results) {
            map.put((Status) row[0], (Long) row[1]);
        }
        return map;
    }
}