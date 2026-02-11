package com.project.rare_x_back.scheduler;

import com.project.rare_x_back.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCancelSellerScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        try {
            log.info("판매자 미발송 자동 주문 취소 스케줄러가 시작되었습니다.");
            orderService.autoCancelSeller();
            log.info("판매자 미발송 자동 주문 취소 스케줄러가 정상 종료 되었습니다.");
        } catch (Exception e) {
            log.error("판매자 미발송 자동 주문 취소 스케줄러 실행 중 에러 발생 : {}", e.getMessage(), e);
        }
    }
}
