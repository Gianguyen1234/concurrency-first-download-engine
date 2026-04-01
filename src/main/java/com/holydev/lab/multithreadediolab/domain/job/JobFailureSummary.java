package com.holydev.lab.multithreadediolab.domain.job;

import java.util.Map;

public record JobFailureSummary(
        long jobId,
        int totalTasks,
        int failedTasks,
        int retriedTasks,
        int successfulAfterRetry,
        Map<String, Integer> failureCounts
) {
}
