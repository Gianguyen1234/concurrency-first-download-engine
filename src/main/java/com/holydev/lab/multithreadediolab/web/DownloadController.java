package com.holydev.lab.multithreadediolab.web;

import com.holydev.lab.multithreadediolab.app.DownloadJobService;
import com.holydev.lab.multithreadediolab.infra.tracking.DownloadJobTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DownloadController {
    private final DownloadJobService jobService;

    public DownloadController(DownloadJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/jobs/start")
    public DownloadJobService.StartJobResponse startJob(
            @RequestParam(defaultValue = "50") int count,
            @RequestParam(defaultValue = "https://picsum.photos/300/300") String baseUrl
    ) {
        return jobService.startJob(count, baseUrl);
    }

    @GetMapping("/jobs/{jobId}")
    public DownloadJobTracker.DownloadJobSnapshot jobStatus(@PathVariable long jobId) {
        return jobService.getJob(jobId);
    }

    @GetMapping("/jobs/{jobId}/tasks")
    public java.util.List<DownloadJobTracker.DownloadTaskSnapshot> jobTasks(@PathVariable long jobId) {
        return jobService.getTasks(jobId);
    }

    @GetMapping("/start-download")
    public DownloadJobService.StartJobResponse legacyStart(@RequestParam(defaultValue = "50") int count) {
        return jobService.startJob(count, "https://picsum.photos/300/300");
    }

    @GetMapping("/download-status")
    public DownloadJobTracker.DownloadJobSnapshot legacyStatus() {
        return jobService.getLatestJob();
    }
}
