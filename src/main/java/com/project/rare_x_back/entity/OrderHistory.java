package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.CurrentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "order_histories")
@EntityListeners(AuditingEntityListener.class)
public class OrderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;                    /// 주문

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CurrentStatus currentStatus;            /// 주문 상태

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;       /// 주문 생성일

    @Column(name = "description")
    private String description;

    public static OrderHistory create(Order order, CurrentStatus status) {
        OrderHistory history = new OrderHistory();
        history.order = order;
        history.currentStatus = status;
        return history;
    }

    public static OrderHistory createCancelHistory(Order order, CurrentStatus status, String description) {
        OrderHistory history = create(order, status);
        history.description = description;
        return history;
    }

}
