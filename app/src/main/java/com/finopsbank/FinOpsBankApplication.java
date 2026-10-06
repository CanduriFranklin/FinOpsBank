package com.finopsbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan; // <-- Import actualize
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.finopsbank")
@EntityScan(basePackages = "com.finopsbank.persistence.entity")
@EnableJpaRepositories(basePackages = "com.finopsbank.persistence.repository")
public class FinOpsBankApplication {

    static void main(String[] args) {
        SpringApplication.run(FinOpsBankApplication.class, args);
    }
}