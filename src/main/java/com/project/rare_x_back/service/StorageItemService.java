package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.StorageItemResponseDto;
import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.StorageItem;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.enums.StorageStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.InspectionRepository;
import com.project.rare_x_back.repository.SaleBidRepository;
import com.project.rare_x_back.repository.StorageItemRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageItemService {

    private final StorageItemRepository storageItemRepository;
    private final UserRepository userRepository;
    private final SaleBidRepository saleBidRepository;
    private final InspectionRepository inspectionRepository;
    private final SearchService searchService;

    // 보관 중 상품 목록 조회
    public Page<StorageItemResponseDto> getMyStorageItems(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<StorageItem> storageItems = storageItemRepository.findByUserUserId(user.getUserId(), pageable);

        // 해당 유저 보관함의 storageId 목록 추출
        List<Long> userStorageIds = storageItems.getContent().stream()
                .map(StorageItem::getStorageId)
                .toList();

        // 반송 요청 중인 storageId를 Set으로 조회 (O(1) lookup)
        Set<Long> releaseRequestedIds = new HashSet<>(
                inspectionRepository.findStorageIdsByStatusAndStorageIds(
                        InspectionStatus.RELEASE_REQUESTED, userStorageIds)
        );

        return storageItems.map(item -> StorageItemResponseDto.from(
                item,
                releaseRequestedIds.contains(item.getStorageId())
        ));
    }

    // 보관 상품 반송 요청 (고객 요청)
    @Transactional
    public void requestRelease(String userEmail, Long storageId) {
        // 1. 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 보관함 조회
        StorageItem storageItem = storageItemRepository.findById(storageId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "보관 상품을 찾을 수 없습니다."));

        // 3. 본인 확인
        if (!storageItem.getUser().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 보관 상품만 반송 요청할 수 있습니다.");
        }

        // 4. 반송 가능 상태 확인 (STORED, ON_SALE, SUSPENDED)
        List<StorageStatus> allowedStatuses = List.of(
                StorageStatus.STORED, StorageStatus.ON_SALE, StorageStatus.SUSPENDED
        );
        if (!allowedStatuses.contains(storageItem.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST,
                    "반송 요청이 불가능한 상태입니다. (현재: " + storageItem.getStatus() + ")");
        }

        // 5. 이미 반송 요청 중인지 확인
        // TODO: 동시 요청 시 레이스 컨디션 가능성 있음. 필요 시 DB 유니크 제약조건 추가 고려
        boolean alreadyRequested = inspectionRepository
                .existsByStorageItemStorageIdAndStatus(storageItem.getStorageId(), InspectionStatus.RELEASE_REQUESTED);
        if (alreadyRequested) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 반송 요청이 접수된 상품입니다. 관리자 처리를 기다려주세요.");
        }

        // 6. ON_SALE 상태면 판매 입찰 자동 취소
        if (storageItem.getStatus() == StorageStatus.ON_SALE) {
            saleBidRepository.cancelByStorageId(storageItem.getStorageId(), BidStatus.CANCELED, BidStatus.OPEN);
            log.info("반송 요청으로 판매 입찰 자동 취소: storageId={}", storageId);
        }

        // 7. Inspection 레코드 생성 (반송 요청)
        Inspection inspection = Inspection.builder()
                .storageItem(storageItem)
                .type(InspectionType.RELEASE)
                .status(InspectionStatus.RELEASE_REQUESTED)
                .build();
        inspectionRepository.save(inspection);

        // 8. Typesense 인덱싱
        searchService.indexInspection(inspection);

        log.info("반송 요청 완료: storageId={}, userId={}, inspectionId={}",
                storageId, user.getUserId(), inspection.getInspectionId());
    }
}
