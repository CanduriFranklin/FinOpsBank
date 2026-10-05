package com.finopsbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.finopsbank")
@EntityScan(basePackages = "com.finopsbank.persistence.entity")
@EnableJpaRepositories(basePackages = "com.finopsbank.persistence.repository")
public class FinOpsBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinOpsBankApplication.class, args);
    }
}
