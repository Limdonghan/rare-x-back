package com.project.rare_x_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // 엔티티 @CreatedDate, @LastModifiedDate 어노테이션 활성화 스위치 역할(시간 자동 기록)
public class RareXBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(RareXBackApplication.class, args);
	}

}
