package com.holydev.lab.multithreadediolab.domain.job;

public record StartJobResponse(long jobId, int count, String baseUrl, String message) {
}
