package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.StorageItemResponseDto;
import com.project.rare_x_back.entity.StorageItem;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.StorageItemRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageItemService {

    private final StorageItemRepository storageItemRepository;
    private final UserRepository userRepository;

    // 보관 중 상품 목록 조회
    public List<StorageItemResponseDto> getMyStorageItems(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<StorageItem> storageItems = storageItemRepository.findByUserUserId(user.getUserId());

        return storageItems.stream()    // 리스트에 담긴 데이터들을 하나씩 꺼내어 처리할 준비
                .map(StorageItemResponseDto::from)  // Entity -> DTO 변환
                .collect(Collectors.toList());  // 최종 리스트로 반환
    }
}
