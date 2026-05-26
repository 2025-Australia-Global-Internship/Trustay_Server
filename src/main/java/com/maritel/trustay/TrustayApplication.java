package com.maritel.trustay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class TrustayApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrustayApplication.class, args);
        log.info("\n\n======================================== ApiApplication started ========================================\n\n");
    }

}
