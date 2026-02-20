package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.InspectionChecklistRequestDto;
import com.project.rare_x_back.dto.response.InspectionChecklistResponseDto;
import com.project.rare_x_back.dto.response.InspectionHistoryDetailResponseDto;
import com.project.rare_x_back.dto.response.InspectionHistoryResponseDto;
import com.project.rare_x_back.dto.response.InspectionResponseDto;
import com.project.rare_x_back.entity.*;
import com.project.rare_x_back.enums.*;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final InspectionChecklistRepository inspectionChecklistRepository;
    private final UserRepository userRepository;
    private final StorageItemRepository storageItemRepository;
    private final OrderService orderService;
    private final SearchService searchService;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * 전체 검수 목록 조회 (타입 무관, 페이징)
     *
     * @param status   검수 상태 (null이면 전체)
     * @param pageable 페이징 정보
     * @return 검수 목록
     */
    public Page<InspectionResponseDto> getAllInspections(InspectionStatus status, Pageable pageable) {
        Page<Inspection> inspections;

        if (status == null) {
            inspections = inspectionRepository.findAllWithDetails(pageable);
        } else {
            inspections = inspectionRepository.findByStatus(status, pageable);
        }

        return inspections.map(InspectionResponseDto::from);
    }

    /**
     * 타입별 검수 목록 조회 (페이징)
     *
     * @param type     검수 타입 (STORAGE, ORDER)
     * @param status   검수 상태 (null이면 전체)
     * @param pageable 페이징 정보
     * @return 검수 목록
     */
    public Page<InspectionResponseDto> getInspectionList(InspectionType type, InspectionStatus status, Pageable pageable) {
        Page<Inspection> inspections;

        if (status == null) {
            inspections = inspectionRepository.findByType(type, pageable);
        } else {
            inspections = inspectionRepository.findByTypeAndStatus(type, status, pageable);
        }

        return inspections.map(InspectionResponseDto::from);
    }

    /**
     * 상태별 검수 건수 조회 (대시보드용)
     *
     * @param type   검수 타입
     * @param status 검수 상태
     * @return 건수
     */
    public long getInspectionCount(InspectionType type, InspectionStatus status) {
        return inspectionRepository.countByTypeAndStatus(type, status);
    }

    /**
     * 검수 상세 조회
     *
     * @param inspectionId 검수 ID
     * @return 검수 상세 정보
     */
    public InspectionResponseDto getInspection(Long inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "검수 정보를 찾을 수 없습니다."
                ));

        return InspectionResponseDto.from(inspection);
    }

    /**
     * 도착 확인 (SHIPPED_TO_WAREHOUSE → PENDING_INSPECTION)
     */
    @Transactional
    public InspectionResponseDto confirmArrival(Long inspectionId) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 상태 확인
        if (inspection.getStatus() != InspectionStatus.SHIPPED_TO_WAREHOUSE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "검수센터로 이동 중인 상태에서만 도착 확인이 가능합니다.");
        }

        // 3. Inspection 상태 변경
        inspection.updateStatus(InspectionStatus.PENDING_INSPECTION);

        // 4. StorageRequest 상태도 함께 변경
        if (inspection.getStorageRequest() != null) {
            inspection.getStorageRequest().updateStatus(StorageRequestStatus.PENDING_INSPECTION);
        }

        // order 상태 변경, order_history 이력 저장
        if (inspection.getOrder() != null) {
            orderService.updateOrderStatus(inspection.getOrder(),CurrentStatus.PENDING_INSPECTION);
        }

        // Typesense 인덱싱
        searchService.indexInspection(inspection);

        return InspectionResponseDto.from(inspection);
    }

    /**
     * 검수 시작 (PENDING_INSPECTION → INSPECTING)
     * - 담당자 배정
     * - 체크리스트 생성
     * - 시작 시간 기록
     */
    @Transactional
    public InspectionResponseDto startInspection(Long inspectionId, Long adminId) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 상태 확인
        if (inspection.getStatus() != InspectionStatus.PENDING_INSPECTION) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "검수 대기 상태에서만 검수를 시작할 수 있습니다.");
        }

        // 3. 담당자(관리자) 조회 및 배정
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "관리자를 찾을 수 없습니다."));
        inspection.assignInspector(admin);

        // 4. 상태 변경 (INSPECTING + started_at 기록)
        inspection.updateStatus(InspectionStatus.INSPECTING);

        // 5. StorageRequest 상태도 함께 변경
        if (inspection.getStorageRequest() != null) {
            inspection.getStorageRequest().updateStatus(StorageRequestStatus.INSPECTING);
        }

        // order 상태 변경, order_history 이력 저장
        if (inspection.getOrder() != null) {
            orderService.updateOrderStatus(inspection.getOrder(), CurrentStatus.INSPECTING);
        }

        // 7. 체크리스트 생성
        InspectionChecklist checklist = InspectionChecklist.builder()
                .inspection(inspection)
                .build();
        inspectionChecklistRepository.save(checklist);

        // Typesense 인덱싱
        searchService.indexInspection(inspection);

        return InspectionResponseDto.from(inspection);
    }

    // 체크리스트 조회
    public InspectionChecklistResponseDto getChecklist(Long inspectionId) {
        // 1. 먼저 검수 존재 확인
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 체크리스트 조회
        InspectionChecklist checklist = inspectionChecklistRepository.findByInspectionId(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "체크리스트를 찾을 수 없습니다."));

        return InspectionChecklistResponseDto.from(checklist);
    }

    // 체크리스트 수정
    @Transactional
    public InspectionChecklistResponseDto updateChecklist(Long inspectionId, InspectionChecklistRequestDto request) {
        // 1. 먼저 검수 존재 확인
        inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 체크리스트 조회
        InspectionChecklist checklist = inspectionChecklistRepository.findByInspectionId(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "체크리스트를 찾을 수 없습니다."));

        checklist.updateChecklist(
                request.getIsAuthentic(),
                request.getIsExteriorGood(),
                request.getIsComponentsComplete(),
                request.getIsPackagingGood(),
                request.getIsUnused()
        );

        return InspectionChecklistResponseDto.from(checklist);
    }

    /**
     * 검수 합격 처리 (INSPECTING → STORAGE 타입: PASSED, ORDER 타입: PASSED)
     * - STORAGE 타입: storage_requests 상태 동기화 + storage_items 생성
     * - ORDER 타입: orders 상태 동기화
     */
    @Transactional
    public InspectionResponseDto passInspection(Long inspectionId) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 상태 확인
        if (inspection.getStatus() != InspectionStatus.INSPECTING) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "검수 진행 중인 건만 합격 처리할 수 있습니다.");
        }

        // 3. inspections 상태 변경 (PASSED + inspected_at 기록)
        inspection.updateStatus(InspectionStatus.PASSED);

        // 4. 타입별 처리
        if (inspection.getType() == InspectionType.STORAGE) {
            StorageRequest storageRequest = inspection.getStorageRequest();

            // NPE 검사
            if (storageRequest == null) {
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "보관 신청 정보를 찾을 수 없습니다.");
            }

            // storage_requests 상태 동기화
            storageRequest.updateStatus(StorageRequestStatus.PASSED);

            // storage_items 레코드 생성
            StorageItem storageItem = StorageItem.builder()
                    .user(storageRequest.getUser())
                    .product(storageRequest.getProduct())
                    .status(StorageStatus.STORED)
                    .expiredAt(LocalDateTime.now().plusDays(180))
                    .build();
            storageItemRepository.save(storageItem);
        }

        if (inspection.getType() == InspectionType.ORDER) {
            Order order = inspection.getOrder();

            // NPE 검사
            if (order == null) {
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다.");
            }

            // order 상태 변경, order_history 이력 저장 (검수 합격)
            orderService.updateOrderStatus(order, CurrentStatus.PASSED);

            /// [추가] 판매자에게 검수 합격 알림 전송
            notificationService.send(
                    order.getSeller().getUserId(),
                    "판매하신 상품(" + order.getProduct().getProductName() + ")이 검수에 합격했습니다.",
                    "/mypage/order", /// 판매 내역 페이지
                    NotificationType.INSPECTION_RESULT,
                    EmailType.INSPECTION_RESULT
            );
        }

        // Typesense 인덱싱
        searchService.indexInspection(inspection);

        return InspectionResponseDto.from(inspection);
    }

    /**
     * 검수 불합격 처리 (INSPECTING → FAILED, RETURN)
     * - STORAGE 타입: storage_requests 상태 동기화
     * - ORDER 타입: orders 상태 동기화
     */
    @Transactional
    public InspectionResponseDto failInspection(Long inspectionId, String failReason) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 상태 확인
        if (inspection.getStatus() != InspectionStatus.INSPECTING) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "검수 진행 중인 건만 불합격 처리할 수 있습니다.");
        }

        // 3. 불합격 처리 (상태 + 사유 + 시간)
        inspection.fail(failReason);

        // 4. 타입별 처리
        if (inspection.getType() == InspectionType.STORAGE) {
            StorageRequest storageRequest = inspection.getStorageRequest();

            // NPE 검사
            if (storageRequest == null) {
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "보관 신청 정보를 찾을 수 없습니다.");
            }

            // storage_requests 상태 동기화
            storageRequest.updateStatus(StorageRequestStatus.RETURN);

            /// [추가] 유저에게 검수 불합격 알림 전송
            notificationService.send(
                    storageRequest.getUser().getUserId(),
                    "보관 신청하신 상품이 검수 불합격되었습니다. (사유: " + failReason + ")",
                    "/mypage/storagerequest", /// 보관 신청 내역 (또는 불합격 상세)
                    NotificationType.INSPECTION_RESULT,
                    EmailType.INSPECTION_RESULT
            );
        }

        // 입찰 주문 건 검수실패 처리
        if (inspection.getType() == InspectionType.ORDER) {
            Order order = orderRepository.findByIdWithLock(inspection.getOrder().getOrderId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다."));

            // 구매자 결제 내역 조회
            Payment payment = paymentRepository.findByOrder_OrderId(order.getOrderId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND, "구매자의 결제 내역을 찾을 수 없습니다."));

            // 이력 저장 및 상태 변경 + 인덱싱
            orderService.finalizeFailInspectionCancellation(order);

            /// [추가] 구매자에게 검수 불합격 및 결제 취소 알림 전송
            notificationService.send(
                    order.getBuyer().getUserId(), /// 구매자 ID
                    "주문하신 상품이 검수 불합격되어 결제가 취소되었습니다.",
                    "/mypage/order", /// 구매 내역
                    NotificationType.ORDER_STATUS,
                    EmailType.INSPECTION_RESULT
            );

            /// [추가] 판매자에게도 알림
            notificationService.send(
                    order.getSeller().getUserId(), /// 판매자 ID
                    "판매하신 상품이 검수 불합격 처리되었습니다. (사유: " + failReason + ")",
                    "/mypage/contract", /// 판매 내역
                    NotificationType.INSPECTION_RESULT,
                    EmailType.INSPECTION_RESULT
            );

            // 환불 금액 (전액 -> 검수 실패 이므로 구매자 귀책 X)
            long cancelAmount = payment.getAmount();

            try {
                paymentService.cancelOnce(
                    order.getOrderId(),
                    cancelAmount,
                    "INSPECTION_FAIL_CANCELED",
                    "INSPECTION_FAIL"
                );
            } catch (Exception e) {
                log.error("결제 취소 중 에러 발생, 전체 로직 롤백.");
                throw e;
            }
        }

        // Typesense 인덱싱
        searchService.indexInspection(inspection);

        return InspectionResponseDto.from(inspection);
    }

    /**
     * 검수 이력 목록 조회 (페이징)
     */
    public Page<InspectionHistoryResponseDto> getInspectionHistory(
            InspectionStatus status, Pageable pageable) {

        Page<Inspection> inspections;

        if (status == null) {
            // 전체 이력 조회 (PASSED + FAILED)
            inspections = inspectionRepository.findHistoryByStatusIn(
                    List.of(InspectionStatus.PASSED, InspectionStatus.FAILED), pageable);
        } else {
            // 상태별 조회
            inspections = inspectionRepository.findHistoryByStatus(status, pageable);
        }

        return inspections.map(InspectionHistoryResponseDto::from);
    }

    /**
     * 검수 이력 상세 조회 (체크리스트 포함)
     */
    public InspectionHistoryDetailResponseDto getInspectionHistoryDetail(Long inspectionId) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 완료된 검수인지 확인 (PASSED 또는 FAILED)
        if (inspection.getStatus() != InspectionStatus.PASSED && inspection.getStatus() != InspectionStatus.FAILED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "완료된 검수 이력만 조회할 수 있습니다.");
        }

        // 3. 체크리스트 조회 (없을 수도 있음)
        InspectionChecklist checklist = inspectionChecklistRepository.findByInspectionId(inspectionId)
                .orElse(null);

        return InspectionHistoryDetailResponseDto.from(inspection, checklist);
    }

    // 검수 패스 후 구매자에게 발송함 PASSED -> SHIPPED (주문 검수 전용)
    @Transactional
    public void deliveryToBuyer (Long inspectionId) {
        // 1. 검수 조회
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "검수 정보를 찾을 수 없습니다."));

        // 2. 주문 검수인지 확인 (보관 검수는 일반 주문이 아닌 보관 신청 건이므로 InspectionType이 order가 아님)
        if (inspection.getType() != InspectionType.ORDER) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "주문 검수 건만 발송 처리할 수 있습니다.");
        }

        // 3. 주문 정보 확인
        Order order = inspection.getOrder();
        if (order == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "주문 정보를 찾을 수 없습니다.");
        }

        // 4. 상태가 검수 통과인지 확인
        if (inspection.getStatus() != InspectionStatus.PASSED) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "검수 통과된 상품만 배송할 수 있습니다.");
        }

        // 5. order 상태 변경, order_history 이력 저장
        orderService.updateOrderStatus(order, CurrentStatus.SHIPPED);
    }

    // ============================================
    // 일괄 처리 메서드
    // ============================================

    /**
     * 일괄 도착 확인 (SHIPPED_TO_WAREHOUSE → PENDING_INSPECTION)
     */
    @Transactional
    public void bulkConfirmArrival(List<Long> inspectionIds) {
        // 1. ID 존재 여부 + 상태 일치 검증
        validateInspectionIds(inspectionIds, InspectionStatus.SHIPPED_TO_WAREHOUSE);

        // 2. 개별 도착확인 처리 (상태 변경 + 연관 엔티티 동기화 + Typesense)
        for (Long id : inspectionIds) {
            confirmArrival(id);
        }
    }

    /**
     * 일괄 검수 시작 (PENDING_INSPECTION → INSPECTING)
     * - 담당자 배정 + 체크리스트 생성
     */
    @Transactional
    public void bulkStartInspection(List<Long> inspectionIds, Long adminId) {
        // 1. ID 존재 여부 + 상태 일치 검증
        validateInspectionIds(inspectionIds, InspectionStatus.PENDING_INSPECTION);

        // 2. 개별 검수시작 처리 (담당자 배정 + 체크리스트 생성 + 상태 변경)
        for (Long id : inspectionIds) {
            startInspection(id, adminId);
        }
    }

    /**
     * 일괄 합격 처리 (INSPECTING → PASSED)
     * - 체크리스트 동일값 일괄 적용 후 합격 처리
     * - STORAGE: storage_items 생성 / ORDER: orders 상태 동기화
     */
    @Transactional
    public void bulkPassInspection(List<Long> inspectionIds, InspectionChecklistRequestDto checklistDto) {
        // 1. ID 존재 여부 + 상태 일치 검증
        validateInspectionIds(inspectionIds, InspectionStatus.INSPECTING);

        // 2. 체크리스트 저장 + 합격 처리
        for (Long id : inspectionIds) {
            if (checklistDto != null) {
                updateChecklist(id, checklistDto);
            }
            passInspection(id);
        }
    }

    /**
     * 일괄 불합격 처리 (INSPECTING → FAILED)
     * - 체크리스트 동일값 일괄 적용 + 동일 사유로 불합격 처리
     * - STORAGE: 반송 처리 / ORDER: 반송 처리
     */
    @Transactional
    public void bulkFailInspection(List<Long> inspectionIds, InspectionChecklistRequestDto checklistDto, String failReason) {
        // 1. ID 존재 여부 + 상태 일치 검증
        validateInspectionIds(inspectionIds, InspectionStatus.INSPECTING);

        // 2. 불합격 사유 필수 검증
        if (failReason == null || failReason.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "불합격 사유는 필수입니다.");
        }

        // 3. 체크리스트 저장 + 불합격 처리
        for (Long id : inspectionIds) {
            if (checklistDto != null) {
                updateChecklist(id, checklistDto);
            }
            failInspection(id, failReason);
        }
    }

    // ============================================
    // 공통 검증 메서드
    // ============================================

    /**
     * 일괄 처리 공통 검증
     * - inspectionIds에 해당하는 검수 건이 모두 존재하는지 확인
     * - 모든 검수 건의 상태가 expectedStatus와 일치하는지 확인
     */
    private void validateInspectionIds(List<Long> inspectionIds, InspectionStatus expectedStatus) {
        List<Inspection> inspections = inspectionRepository.findAllByIdWithDetails(inspectionIds);

        // ID 개수 불일치 → 존재하지 않는 검수 건 포함
        if (inspections.size() != inspectionIds.size()) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 검수 건이 포함되어 있습니다.");
        }

        // 상태 불일치 → 해당 검수 번호 표시
        for (Inspection inspection : inspections) {
            if (inspection.getStatus() != expectedStatus) {
                throw new CustomException(ErrorCode.INVALID_REQUEST,
                        "선택한 검수 건의 상태가 일치하지 않습니다. (VER-"
                                + String.format("%03d", inspection.getInspectionId()) + ")");
            }
        }
    }
}
