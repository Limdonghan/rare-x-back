package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.InspectionChecklistRequestDto;
import com.project.rare_x_back.dto.response.InspectionChecklistResponseDto;
import com.project.rare_x_back.dto.response.InspectionResponseDto;
import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.InspectionChecklist;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.enums.StorageRequestStatus;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.InspectionChecklistRepository;
import com.project.rare_x_back.repository.InspectionRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final InspectionChecklistRepository inspectionChecklistRepository;
    private final UserRepository userRepository;

    /**
     * 전체 검수 목록 조회 (타입 무관)
     *
     * @param status 검수 상태 (null이면 전체)
     * @return 검수 목록
     */
    public List<InspectionResponseDto> getAllInspections(InspectionStatus status) {
        List<Inspection> inspections;

        if (status == null) {
            inspections = inspectionRepository.findAllWithDetails();
        } else {
            inspections = inspectionRepository.findByStatusWithDetails(status);
        }

        return inspections.stream()
                .map(InspectionResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 타입별 검수 목록 조회
     *
     * @param type 검수 타입 (STORAGE, ORDER)
     * @param status 검수 상태 (null이면 전체)
     * @return 검수 목록
     */
    public List<InspectionResponseDto> getInspectionList(InspectionType type, InspectionStatus status) {
        List<Inspection> inspections;   // 리스트 초기화를 위한 변수 선언

        if (status == null) {
            inspections = inspectionRepository.findByTypeWithDetails(type);
        } else {
            inspections = inspectionRepository.findByTypeAndStatusWithDetails(type, status);
        }

        return inspections.stream()
                .map(InspectionResponseDto::from)
                .collect(Collectors.toList());
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

        // 6. 체크리스트 생성
        InspectionChecklist checklist = InspectionChecklist.builder()
                .inspection(inspection)
                .build();
        inspectionChecklistRepository.save(checklist);

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
        Inspection inspection = inspectionRepository.findById(inspectionId)
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
}
