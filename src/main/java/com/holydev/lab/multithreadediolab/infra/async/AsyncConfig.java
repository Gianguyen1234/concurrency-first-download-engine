package com.holydev.lab.multithreadediolab.infra.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "imageTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        // Với I/O-bound, số thread thường có thể lớn hơn số core
        // vì nhiều thread sẽ dành phần lớn thời gian để chờ mạng / chờ ghi file.
        int corePool = Math.max(2, cores * 2);
        int maxPool = Math.max(corePool, cores * 5);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePool);
        executor.setMaxPoolSize(maxPool);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("IO-Lab-Thread-");
        executor.initialize();
        return executor;
    }
}
