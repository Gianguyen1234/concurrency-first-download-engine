package com.holydev.lab.multithreadediolab.app;

import com.holydev.lab.multithreadediolab.domain.benchmark.BenchmarkReport;
import com.holydev.lab.multithreadediolab.domain.job.CancelJobResponse;
import com.holydev.lab.multithreadediolab.domain.job.DownloadJobSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.DownloadTaskSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.JobFailureSummary;
import com.holydev.lab.multithreadediolab.domain.job.StartJobResponse;
import com.holydev.lab.multithreadediolab.infra.download.ImageDownloaderService;
import com.holydev.lab.multithreadediolab.infra.tracking.DownloadJobTracker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        validateBaseUrl(baseUrl);
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

    public List<DownloadTaskSnapshot> getTasks(long jobId) {
        return tracker.getTasks(jobId);
    }

    public JobFailureSummary getFailureSummary(long jobId) {
        return tracker.getFailureSummary(jobId);
    }

    public CancelJobResponse cancelJob(long jobId) {
        CancelJobResponse response = tracker.cancelJob(jobId);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "job not found");
        }
        return response;
    }

    public BenchmarkReport runBenchmark(int count, String baseUrl, long pollIntervalMs, long timeoutMillis) {
        long safePollIntervalMs = Math.max(50, pollIntervalMs);
        long safeTimeoutMillis = Math.max(safePollIntervalMs, timeoutMillis);

        StartJobResponse startJobResponse = startJob(count, baseUrl);
        long jobId = startJobResponse.jobId();
        long startedAtNs = System.nanoTime();
        boolean timedOut = false;
        boolean cancelledOnTimeout = false;

        while (true) {
            DownloadJobSnapshot snapshot = getJob(jobId);
            if (snapshot == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "job not found during benchmark");
            }

            if (snapshot.finished()) {
                return new BenchmarkReport(
                        jobId,
                        count,
                        baseUrl,
                        safePollIntervalMs,
                        safeTimeoutMillis,
                        false,
                        false,
                        snapshot,
                        getFailureSummary(jobId),
                        "Benchmark completed successfully."
                );
            }

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNs);
            if (elapsedMillis >= safeTimeoutMillis) {
                timedOut = true;
                cancelJob(jobId);
                cancelledOnTimeout = true;
                DownloadJobSnapshot finalSnapshot = getJob(jobId);
                return new BenchmarkReport(
                        jobId,
                        count,
                        baseUrl,
                        safePollIntervalMs,
                        safeTimeoutMillis,
                        timedOut,
                        cancelledOnTimeout,
                        finalSnapshot,
                        getFailureSummary(jobId),
                        "Benchmark timed out before the job finished. The job was cancelled to stop remaining queued work."
                );
            }

            sleepQuietly(safePollIntervalMs);
        }
    }

    private void validateBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseUrl must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseUrl is not a valid URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseUrl must start with http:// or https://");
        }

        if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseUrl must be an absolute HTTP(S) URL");
        }
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "benchmark wait was interrupted");
        }
    }
}
