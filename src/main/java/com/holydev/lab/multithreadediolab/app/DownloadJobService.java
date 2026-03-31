package com.holydev.lab.multithreadediolab.app;

import com.holydev.lab.multithreadediolab.infra.download.ImageDownloaderService;
import com.holydev.lab.multithreadediolab.infra.tracking.DownloadJobTracker;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DownloadJobService {

    public record StartJobResponse(long jobId, int count, String baseUrl, String message) {
    }

    private final DownloadJobTracker tracker;
    private final ImageDownloaderService imageDownloaderService;

    public DownloadJobService(DownloadJobTracker tracker, ImageDownloaderService imageDownloaderService) {
        this.tracker = tracker;
        this.imageDownloaderService = imageDownloaderService;
    }

    public StartJobResponse startJob(int count, String baseUrl) {
        long jobId = tracker.startJob(baseUrl, count);

        for (int i = 0; i < count; i++) {
            String finalUrl = baseUrl + "?random=" + i;
            tracker.registerTask(jobId, i, finalUrl);
            imageDownloaderService.downloadImage(finalUrl, i, jobId);
        }

        return new StartJobResponse(
                jobId,
                count,
                baseUrl,
                "Job started. Use /jobs/" + jobId + " and /jobs/" + jobId + "/tasks to inspect progress."
        );
    }

    public DownloadJobTracker.DownloadJobSnapshot getJob(long jobId) {
        return tracker.getJob(jobId);
    }

    public DownloadJobTracker.DownloadJobSnapshot getLatestJob() {
        return tracker.getLatestJob();
    }

    public List<DownloadJobTracker.DownloadTaskSnapshot> getTasks(long jobId) {
        return tracker.getTasks(jobId);
    }
}
