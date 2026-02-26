package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /// 사용자별 알림 리스트
    List<Notification> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /// 읽지 않은 알림 카운트
    long countByUser_UserIdAndIsReadFalse(Long userId);

    /// (성능 최적화) 사용자의 '읽지 않은 알림' 전체를 한 번의 UPDATE 쿼리로 '읽음' 처리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}
