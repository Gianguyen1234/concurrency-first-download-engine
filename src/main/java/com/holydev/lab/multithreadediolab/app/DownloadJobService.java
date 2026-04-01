package com.holydev.lab.multithreadediolab.app;

import com.holydev.lab.multithreadediolab.domain.job.DownloadJobSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.DownloadTaskSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.StartJobResponse;
import com.holydev.lab.multithreadediolab.infra.download.ImageDownloaderService;
import com.holydev.lab.multithreadediolab.infra.tracking.DownloadJobTracker;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DownloadJobService {

    private final DownloadJobTracker tracker;
    private final ImageDownloaderService imageDownloaderService;

    public DownloadJobService(DownloadJobTracker tracker, ImageDownloaderService imageDownloaderService) {
        this.tracker = tracker;
        this.imageDownloaderService = imageDownloaderService;
    }

    // Đây là lớp điều phối use case "start job":
    // 1) tạo job
    // 2) đăng ký từng task vào tracker
    // 3) đẩy từng task sang worker async để chạy nền
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

    // Các hàm read-only bên dưới chỉ lấy snapshot đã được tracker tổng hợp sẵn.
    public DownloadJobSnapshot getJob(long jobId) {
        return tracker.getJob(jobId);
    }

    public DownloadJobSnapshot getLatestJob() {
        return tracker.getLatestJob();
    }

    public List<DownloadTaskSnapshot> getTasks(long jobId) {
        return tracker.getTasks(jobId);
    }
}
