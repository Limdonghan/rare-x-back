package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
