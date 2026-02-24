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

import java.util.List;
import java.util.stream.Collectors;

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
    public List<StorageItemResponseDto> getMyStorageItems(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<StorageItem> storageItems = storageItemRepository.findByUserUserId(user.getUserId());

        return storageItems.stream()    // 리스트에 담긴 데이터들을 하나씩 꺼내어 처리할 준비
                .map(StorageItemResponseDto::from)  // Entity -> DTO 변환
                .collect(Collectors.toList());  // 최종 리스트로 반환
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

        // 5. ON_SALE 상태면 판매 입찰 자동 취소
        if (storageItem.getStatus() == StorageStatus.ON_SALE) {
            saleBidRepository.cancelByStorageId(storageItem.getStorageId(), BidStatus.CANCELED, BidStatus.OPEN);
            log.info("반송 요청으로 판매 입찰 자동 취소: storageId={}", storageId);
        }

        // 5-2. 이미 반송 요청 중인지 확인
        boolean alreadyRequested = inspectionRepository
                .existsByStorageItemStorageIdAndStatus(storageItem.getStorageId(), InspectionStatus.RELEASE_REQUESTED);
        if (alreadyRequested) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 반송 요청이 접수된 상품입니다. 관리자 처리를 기다려주세요.");
        }

        // 6. Inspection 레코드 생성 (반송 요청)
        Inspection inspection = Inspection.builder()
                .storageItem(storageItem)
                .type(InspectionType.RELEASE)
                .status(InspectionStatus.RELEASE_REQUESTED)
                .build();
        inspectionRepository.save(inspection);

        // 7. Typesense 인덱싱
        searchService.indexInspection(inspection);

        log.info("반송 요청 완료: storageId={}, userId={}, inspectionId={}",
                storageId, user.getUserId(), inspection.getInspectionId());
    }
}
