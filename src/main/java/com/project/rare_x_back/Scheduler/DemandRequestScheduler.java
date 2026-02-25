package com.project.rare_x_back.scheduler;

import com.project.rare_x_back.entity.ProductDemandRequest;
import com.project.rare_x_back.repository.ProductDemandRequestRepository;
import com.project.rare_x_back.service.S3ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemandRequestScheduler {

    private final ProductDemandRequestRepository demandRequestRepository;
    private final S3ImageService s3ImageService;

    // 매일 새벽 3시
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldDemandRequest() {

        LocalDateTime date = LocalDateTime.now().minusDays(30);

        List <ProductDemandRequest> oldList = demandRequestRepository.findByCreatedAtBefore(date);

        log.info("30일 지난 상품 등록 요청 {}건 삭제 시작", oldList.size());

        for (ProductDemandRequest demand : oldList) {
            //s3 삭제
            if (demand.getImageUrl() != null) {
                try {
                    s3ImageService.deleteImageByUrl(demand.getImageUrl());
                } catch (Exception e) {
                    log.error("s3 이미지 삭제 실패 - demandId {}, imageUrl : {}", demand.getDemandId(), demand.getImageUrl(), e);
                }
            }
            // DB 삭제
            demandRequestRepository.delete(demand);
        }

        log.info("30일 지난 상품 등록 요청 삭제 완료");

    }
}
