package com.project.rare_x_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing  // 엔티티 @CreatedDate, @LastModifiedDate 어노테이션 활성화 전역 스위치 역할(시간 자동 기록)
@SpringBootApplication
public class RareXBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(RareXBackApplication.class, args);
	}

}
