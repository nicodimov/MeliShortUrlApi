package com.melishorturlapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.melishorturlapi")
public class MeliShortUrlApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeliShortUrlApiApplication.class, args);
    }
}