package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name="message")
    private String message;

    @Column(name = "url")
    private String url;         /// 관련 URL 저장

    @Column(name = "is_read")
    private boolean isRead;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public void isReadUpdate(boolean read) {
        this.isRead = read;
    }


}
