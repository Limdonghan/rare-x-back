package com.project.rare_x_back.Scheduler;

import com.project.rare_x_back.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoPurchaseConfirmScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 0 */6 * * *" )
    public void run() {
        orderService.autoConfirmPurchase();
    }

}
