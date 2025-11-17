package com.bluffmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BluffMasterApplication {
    public static void main(String[] args) {
        SpringApplication.run(BluffMasterApplication.class, args);
    }
}

