package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.AdminDailyRevenueResponseDto;
import com.project.rare_x_back.dto.response.AdminDashboardResponseDto;
import com.project.rare_x_back.repository.AdminDashboardRepository;
import com.project.rare_x_back.repository.InspectionRepository;
import com.project.rare_x_back.repository.OrderRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final AdminDashboardRepository dashboardRepository;
    private final OrderRepository orderRepository;
    private final InspectionRepository inspectionRepository;
    private final UserRepository userRepository;

    public AdminDashboardResponseDto getSummary(String dateStr) {
        LocalDate date = (dateStr == null || dateStr.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(dateStr);

        String d = date.toString();

        long buyerNetFee = Optional.ofNullable(dashboardRepository.sumNetBuyerFeeByDate(d)).orElse(0L);
        long sellerFee   = Optional.ofNullable(dashboardRepository.sumSellerFeeByDate(d)).orElse(0L);

        // 여기서 "오늘 매출" 정의 = buyerNetFee + sellerFee (+패널티 순이익까지 넣고 싶으면 여기서 추가)
        long todayRevenue = buyerNetFee + sellerFee;

        long todayOrderCount = Optional.ofNullable(orderRepository.countOrdersByDate(d)).orElse(0L);
        long pendingInspectionCount = Optional.ofNullable(inspectionRepository.countPendingInspections()).orElse(0L);
        long activeUserCount = Optional.ofNullable(userRepository.countActiveUsers()).orElse(0L);

        return AdminDashboardResponseDto.builder()
                .date(d)
                .todayRevenue(todayRevenue)
                .todayOrderCount(todayOrderCount)
                .pendingInspectionCount(pendingInspectionCount)
                .activeUserCount(activeUserCount)
                .build();
    }

    public List<AdminDailyRevenueResponseDto> getDailyRevenue(String startDate, String endDate) {
        // 1) 구매 순수수료 일별
        List<Map<String, Object>> buyerRows = dashboardRepository.sumNetBuyerFeeDaily(startDate, endDate);
        // 2) 판매 수수료 일별
        List<Map<String, Object>> sellerRows = dashboardRepository.sumSellerFeeDaily(startDate, endDate);

        Map<String, Long> revenueMap = new HashMap<>();

        for (Map<String, Object> r : buyerRows) {
            String d = String.valueOf(r.get("d"));
            long v = ((Number) r.get("net_buyer_fee")).longValue();
            revenueMap.put(d, revenueMap.getOrDefault(d, 0L) + v);
        }
        for (Map<String, Object> r : sellerRows) {
            String d = String.valueOf(r.get("d"));
            long v = ((Number) r.get("seller_fee")).longValue();
            revenueMap.put(d, revenueMap.getOrDefault(d, 0L) + v);
        }

        // 날짜 범위 전체를 채워서(빈 날 0) 프론트에서 그래프 깔끔하게
        LocalDate s = LocalDate.parse(startDate);
        LocalDate e = LocalDate.parse(endDate);

        List<AdminDailyRevenueResponseDto> result = new ArrayList<>();
        for (LocalDate cur = s; !cur.isAfter(e); cur = cur.plusDays(1)) {
            String d = cur.toString();
            result.add(new AdminDailyRevenueResponseDto(d, revenueMap.getOrDefault(d, 0L)));
        }
        return result;
    }
}
