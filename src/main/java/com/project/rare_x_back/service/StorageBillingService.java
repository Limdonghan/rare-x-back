package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.StorageItem;
import com.project.rare_x_back.entity.StoragePayment;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.enums.StorageStatus;
import com.project.rare_x_back.repository.SaleBidRepository;
import com.project.rare_x_back.repository.StorageItemRepository;
import com.project.rare_x_back.repository.StoragePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageBillingService {

    private final StoragePaymentRepository storagePaymentRepository;
    private final StorageItemRepository storageItemRepository;
    private final SaleBidRepository saleBidRepository;
    private final PaymentService paymentService;

    /**
     * 결제 시도 (건별 트랜잭션)
     * 성공/실패 모두 하나의 트랜잭션으로 처리
     */
    @Transactional
    public void attemptPayment(StoragePayment payment, StorageItem item) {
        try {
            String tossPaymentKey = paymentService.payStorageFeeWithBillingKey(
                    item.getUser().getUserId(),
                    payment.getAmount()
            );

            // 성공
            payment.markSuccess(tossPaymentKey);
            storagePaymentRepository.save(payment);
            log.info("보관료 결제 성공: storageId={}, userId={}",
                    item.getStorageId(), item.getUser().getUserId());

        } catch (Exception e) {
            // 실패
            payment.markFailed();
            storagePaymentRepository.save(payment);
            log.warn("보관료 결제 실패: storageId={}, retryCount={}",
                    item.getStorageId(), payment.getRetryCount());

            // 3회 실패 → SUSPENDED 처리
            if (!payment.canRetry()) {
                suspendStorageItem(item);
            }
        }
    }

    /**
     * 보관함 판매 중지 처리
     * - 보관함 상태: SUSPENDED
     * - 해당 보관함의 판매 입찰: CANCELED
     * 하나의 트랜잭션으로 묶여서 부분 실패 시 전체 롤백
     */
    @Transactional
    public void suspendStorageItem(StorageItem item) {
        item.updateStatus(StorageStatus.SUSPENDED);
        storageItemRepository.save(item);

        // 해당 보관함의 OPEN 상태 판매 입찰 취소
        saleBidRepository.cancelByStorageId(item.getStorageId(), BidStatus.CANCELED, BidStatus.OPEN);

        log.warn("보관함 판매 중지: storageId={}, userId={}",
                item.getStorageId(), item.getUser().getUserId());

        // TODO: SUSPENDED 시 사용자 알림 발송
    }
}