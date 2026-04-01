package com.holydev.lab.multithreadediolab.domain.job;

public record DownloadTaskSnapshot(
        int index,
        String url,
        TaskStatus status,
        long bytes,
        long millis,
        String contentType,
        String error,
        String threadName
) {
}
