package com.holydev.lab.multithreadediolab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ConcurrencyFirstDownloadEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcurrencyFirstDownloadEngineApplication.class, args);
    }
}
