package com.project.rare_x_back.scheduler;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.entity.StorageItem;
import com.project.rare_x_back.entity.StoragePayment;
import com.project.rare_x_back.enums.StoragePaymentStatus;
import com.project.rare_x_back.enums.StorageStatus;
import com.project.rare_x_back.repository.StorageItemRepository;
import com.project.rare_x_back.repository.StoragePaymentRepository;
import com.project.rare_x_back.service.StorageBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageFeeScheduler {

    private final StorageItemRepository storageItemRepository;
    private final StoragePaymentRepository storagePaymentRepository;
    private final StorageBillingService storageBillingService;

    private static final int MAX_RETRY = 3;

    /**
     * 매일 00:30 실행
     * 1) 신규 과금 대상 처리
     * 2) 실패 건 재시도
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void run() {
        log.info("보관료 자동결제 스케줄러 시작");

        try {
            processNewBillings();
            processRetries();
        } catch (Exception e) {
            log.error("보관료 스케줄러 실행 중 에러: {}", e.getMessage(), e);
        }

        log.info("보관료 자동결제 스케줄러 종료");
    }

    /**
     * 신규 과금 대상 처리
     * 180일 만료 + 활성 상태 보관함 → 결제 레코드 생성 및 결제 시도
     */
    private void processNewBillings() {
        List<StorageItem> expiredItems = storageItemRepository.findExpiredByStatuses(
                LocalDateTime.now(),
                List.of(StorageStatus.STORED, StorageStatus.ON_SALE)
        );

        log.info("신규 과금 대상: {}건", expiredItems.size());

        for (StorageItem item : expiredItems) {
            try {
                // 과금 시작일 계산
                LocalDate periodStart = calculatePeriodStart(item);

                // 과금 시작일이 아직 안 왔으면 스킵
                if (periodStart.isAfter(LocalDate.now())) {
                    continue;
                }

                // 중복 체크
                if (storagePaymentRepository.existsByStorageItem_StorageIdAndBillingPeriodStart(
                        item.getStorageId(), periodStart)) {
                    continue;
                }

                // 결제 레코드 생성
                StoragePayment payment = StoragePayment.builder()
                        .storageItem(item)
                        .user(item.getUser())
                        .amount(FeeCalculator.STORAGE_FEE_PER_MONTH)
                        .billingPeriodStart(periodStart)
                        .billingPeriodEnd(periodStart.plusDays(30))
                        .build();
                storagePaymentRepository.save(payment);

                // 결제 시도 (트랜잭션 적용)
                storageBillingService.attemptPayment(payment, item);

            } catch (Exception e) {
                log.error("보관함 {} 과금 처리 실패: {}", item.getStorageId(), e.getMessage());
            }
        }
    }

    /**
     * 실패 건 재시도
     * FAILED 상태 & retry_count < 3 → 재시도
     * 3회 실패 시 SUSPENDED 처리
     */
    private void processRetries() {
        List<StoragePayment> retryTargets = storagePaymentRepository
                .findByStatusAndRetryCountLessThan(StoragePaymentStatus.FAILED, MAX_RETRY);

        log.info("재시도 대상: {}건", retryTargets.size());

        for (StoragePayment payment : retryTargets) {
            try {
                payment.resetForRetry();
                storagePaymentRepository.save(payment);
                // 결제 시도 (트랜잭션 적용)
                storageBillingService.attemptPayment(payment, payment.getStorageItem());
            } catch (Exception e) {
                log.error("재시도 실패 - storagePaymentId={}: {}",
                        payment.getStoragePaymentId(), e.getMessage());
            }
        }
    }

    /**
     * 과금 시작일 계산
     * - 최근 성공 결제가 있으면 → 그 결제의 billingPeriodEnd
     * - 없으면 → expiredAt (최초 180일 만료일)
     */
    private LocalDate calculatePeriodStart(StorageItem item) {
        return storagePaymentRepository
                .findTopByStorageItem_StorageIdAndStatusOrderByBillingPeriodEndDesc(
                        item.getStorageId(), StoragePaymentStatus.SUCCESS)
                .map(lastPayment -> lastPayment.getBillingPeriodEnd())
                .orElse(item.getExpiredAt().toLocalDate());
    }
}