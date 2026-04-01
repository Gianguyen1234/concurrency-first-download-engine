package com.holydev.lab.multithreadediolab.domain.job;

import com.holydev.lab.multithreadediolab.domain.download.FailureType;

public record DownloadTaskSnapshot(
        int index,
        String url,
        TaskStatus status,
        long bytes,
        long millis,
        int retryCount,
        String contentType,
        String error,
        FailureType failureType,
        String threadName
) {
}
