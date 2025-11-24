package com.nixo.fde.slackbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FdeSlackbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FdeSlackbotApplication.class, args);
    }
}
