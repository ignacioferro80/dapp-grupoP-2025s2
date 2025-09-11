package com.dapp.sistema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
		scanBasePackages = {
				"com.dapp.sistema",          // tu app
				"crypto.middleware"          // controllers, services, security, utils
		}
)
@EnableJpaRepositories(basePackages = {
		"crypto.middleware.repositories" // <--- muy importante
})
@EntityScan(basePackages = {
		"crypto.middleware.model"        // <--- entidades JPA (User, etc.)
})
public class SistemaApplication {
	public static void main(String[] args) {
		SpringApplication.run(SistemaApplication.class, args);
	}
}
