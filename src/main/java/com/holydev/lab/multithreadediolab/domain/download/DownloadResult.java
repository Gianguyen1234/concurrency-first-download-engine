package com.holydev.lab.multithreadediolab.domain.download;

public record DownloadResult(
        int index,
        boolean ok,
        long bytes,
        long millis,
        String contentType,
        String error,
        FailureType failureType,
        int retryCount
) {
}
