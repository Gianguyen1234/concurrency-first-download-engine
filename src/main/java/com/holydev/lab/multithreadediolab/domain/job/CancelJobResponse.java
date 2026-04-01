package com.holydev.lab.multithreadediolab.domain.job;

public record CancelJobResponse(
        long jobId,
        boolean cancelled,
        int cancelledTasks,
        String message
) {
}
