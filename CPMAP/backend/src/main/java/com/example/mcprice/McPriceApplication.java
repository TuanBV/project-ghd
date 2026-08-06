package com.example.mcprice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class McPriceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McPriceApplication.class, args);
    }
}
