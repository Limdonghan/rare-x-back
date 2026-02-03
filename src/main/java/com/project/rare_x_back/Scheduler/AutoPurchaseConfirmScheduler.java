package com.project.rare_x_back.scheduler;

import com.project.rare_x_back.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPurchaseConfirmScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 0 */6 * * *")
    public void run() {
        try {
            log.info("자동 구매 확정 스케줄러가 시작되었습니다.");
            orderService.autoConfirmPurchase();
            log.info("자동 구매 확정 스케줄러 정상 종료되었습니다.");
        } catch (Exception e) {
            log.error("자동 구매 확정 스케줄러 실행 중 에러 발생 : {}", e.getMessage(), e);
        }
    }

}
