package com.project.rare_x_back;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.TimeZone;
import java.util.concurrent.Executor;
@EnableJpaAuditing  // 엔티티 @CreatedDate, @LastModifiedDate 어노테이션 활성화 전역 스위치 역할(시간 자동 기록)
@SpringBootApplication
@EnableScheduling
@EnableAsync	///  비동기 처리
public class RareXBackApplication {

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);	/// 기본 최대 5
		executor.setMaxPoolSize(10);	/// Max 10
		executor.setQueueCapacity(500);	/// 대기줄 500
		executor.setThreadNamePrefix("Async-");
		executor.initialize();
		return executor;
	}

	@PostConstruct
	public void started(){
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}
	public static void main(String[] args) {
		SpringApplication.run(RareXBackApplication.class, args);
	}

}
