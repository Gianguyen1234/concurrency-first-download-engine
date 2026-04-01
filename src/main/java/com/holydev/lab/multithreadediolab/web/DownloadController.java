package com.holydev.lab.multithreadediolab.web;

import com.holydev.lab.multithreadediolab.app.DownloadJobService;
import com.holydev.lab.multithreadediolab.domain.job.DownloadJobSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.DownloadTaskSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.JobFailureSummary;
import com.holydev.lab.multithreadediolab.domain.job.StartJobResponse;
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

    // Endpoint mới: tạo một download job và trả jobId ngay, không chờ tải xong.
    @GetMapping("/jobs/start")
    public StartJobResponse startJob(
            @RequestParam(defaultValue = "50") int count,
            @RequestParam(defaultValue = "https://picsum.photos/300/300") String baseUrl
    ) {
        return jobService.startJob(count, baseUrl);
    }

    // Xem snapshot tổng quan của 1 job: tổng task, queued, running, completed, ok, fail, throughput...
    @GetMapping("/jobs/{jobId}")
    public DownloadJobSnapshot jobStatus(@PathVariable long jobId) {
        return jobService.getJob(jobId);
    }

    // Xem chi tiết từng task trong job để biết task nào đang queue/running/success/failed.
    @GetMapping("/jobs/{jobId}/tasks")
    public java.util.List<DownloadTaskSnapshot> jobTasks(@PathVariable long jobId) {
        return jobService.getTasks(jobId);
    }

    // Gom nhanh thong ke fail/retry cua mot job de doc tinh hinh reliability.
    @GetMapping("/jobs/{jobId}/failure-summary")
    public JobFailureSummary jobFailureSummary(@PathVariable long jobId) {
        return jobService.getFailureSummary(jobId);
    }
}
