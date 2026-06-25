package com.solofounder.horseracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HorsesRacingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HorsesRacingApplication.class, args);
    }

}
