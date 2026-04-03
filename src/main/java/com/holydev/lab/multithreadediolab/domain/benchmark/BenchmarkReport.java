package com.holydev.lab.multithreadediolab.domain.benchmark;

import com.holydev.lab.multithreadediolab.domain.job.DownloadJobSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.JobFailureSummary;

public record BenchmarkReport(
        long jobId,
        int count,
        String baseUrl,
        long pollIntervalMs,
        long timeoutMillis,
        boolean timedOut,
        boolean cancelledOnTimeout,
        DownloadJobSnapshot jobSnapshot,
        JobFailureSummary failureSummary,
        String message
) {
}
