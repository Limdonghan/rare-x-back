package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.InspectionResponseDto;
import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.InspectionRepository;
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
        List<Inspection> inspections;   // 빈값 선언

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
     * @param type 검수 타입
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
}
