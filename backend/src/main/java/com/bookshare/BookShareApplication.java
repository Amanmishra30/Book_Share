package com.bookshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookShareApplication.class, args);
    }
}
