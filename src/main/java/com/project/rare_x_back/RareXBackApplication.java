package com.project.rare_x_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RareXBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(RareXBackApplication.class, args);
	}

}
