package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /// 사용자별 알림 리스트
    List<Notification> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /// 읽지 않은 알림 카운트
    long countByUser_UserIdAndIsReadFalse(Long userId);
}
