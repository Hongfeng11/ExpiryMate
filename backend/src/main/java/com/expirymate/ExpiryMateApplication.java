package com.expirymate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExpiryMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpiryMateApplication.class, args);
    }
}
