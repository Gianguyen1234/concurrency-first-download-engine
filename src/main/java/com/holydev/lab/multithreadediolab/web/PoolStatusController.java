package com.holydev.lab.multithreadediolab.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadPoolExecutor;

@RestController
public class PoolStatusController {

    private final ThreadPoolTaskExecutor imageTaskExecutor;

    public PoolStatusController(@Qualifier("imageTaskExecutor") ThreadPoolTaskExecutor imageTaskExecutor) {
        this.imageTaskExecutor = imageTaskExecutor;
    }

    public record PoolStatus(
            int corePoolSize,
            int maxPoolSize,
            int poolSize,
            int activeCount,
            int queueSize,
            long completedTaskCount,
            long submittedTaskCount,
            long unfinishedTaskEstimate,
            String meaning
    ) {
    }

    @GetMapping("/pool-status")
    public PoolStatus poolStatus() {
        ThreadPoolExecutor executor = imageTaskExecutor.getThreadPoolExecutor();
        int activeCount = executor.getActiveCount();
        int queueSize = executor.getQueue().size();

        return new PoolStatus(
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getPoolSize(),
                activeCount,
                queueSize,
                executor.getCompletedTaskCount(),
                executor.getTaskCount(),
                activeCount + queueSize,
                "poolSize=tong so thread dang ton tai; activeCount=so thread dang xu ly task (approx); " +
                        "queueSize=so task dang cho trong hang doi; completedTaskCount=tong task da xong; " +
                        "submittedTaskCount=tong task da nop vao executor; unfinishedTaskEstimate~activeCount+queueSize"
        );
    }
}
