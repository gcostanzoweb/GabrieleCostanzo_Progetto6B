package com.gabriele.costanzo.heartbeatfd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HeartbeatFaultDetectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeartbeatFaultDetectorApplication.class, args);
    }

}
