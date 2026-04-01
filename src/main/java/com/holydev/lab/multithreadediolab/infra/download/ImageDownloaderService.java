package com.holydev.lab.multithreadediolab.infra.download;

import com.holydev.lab.multithreadediolab.domain.download.DownloadResult;
import com.holydev.lab.multithreadediolab.domain.download.FailureType;
import com.holydev.lab.multithreadediolab.infra.tracking.DownloadJobTracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ImageDownloaderService {
    private final DownloadJobTracker tracker;

    // Luu file ve thu muc downloads trong project de de kiem tra tren may local.
    private final String SAVE_DIR = System.getProperty("user.dir") + File.separator + "downloads" + File.separator;
    private final RestTemplate restTemplate;
    private final int maxRetries;
    private final long retryBackoffMs;

    public ImageDownloaderService(
            DownloadJobTracker tracker,
            @Value("${app.download.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${app.download.read-timeout-ms:10000}") int readTimeoutMs,
            @Value("${app.download.max-retries:2}") int maxRetries,
            @Value("${app.download.retry-backoff-ms:300}") long retryBackoffMs
    ) {
        this.tracker = tracker;
        this.restTemplate = createRestTemplate(connectTimeoutMs, readTimeoutMs);
        this.maxRetries = Math.max(0, maxRetries);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    // @Async nghia la method nay khong chay tren thread HTTP request,
    // ma chay tren thread pool "imageTaskExecutor".
    @Async("imageTaskExecutor")
    public CompletableFuture<DownloadResult> downloadImage(String urlString, int index, long jobId) {
        String threadName = Thread.currentThread().getName();
        long taskStartNs = System.nanoTime();

        // Vua vao worker thi danh dau task tu QUEUED -> RUNNING.
        tracker.markTaskRunning(jobId, index, threadName);

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                DownloadResult result = tryDownload(urlString, index, taskStartNs, attempt, threadName);
                tracker.recordResult(jobId, result, threadName);
                return CompletableFuture.completedFuture(result);
            } catch (Exception e) {
                FailureType failureType = classifyFailure(e);
                long millis = (System.nanoTime() - taskStartNs) / 1_000_000;
                String err = e.getClass().getSimpleName() + ": " + e.getMessage();

                if (shouldRetry(failureType) && attempt < maxRetries) {
                    int nextRetryCount = attempt + 1;
                    System.err.println("Retrying index " + index + " after " + failureType +
                            " (retry " + nextRetryCount + "/" + maxRetries + ")");
                    tracker.markTaskRetry(jobId, index, nextRetryCount, threadName, failureType, err);
                    sleepBeforeRetry();
                    continue;
                }

                System.err.println("Error at index " + index + ": " + err);
                DownloadResult result = new DownloadResult(index, false, 0, millis, null, err, failureType, attempt);
                // Exception duoc quy ve mot DownloadResult that bai de pipeline khong bi dut.
                tracker.recordResult(jobId, result, threadName);
                return CompletableFuture.completedFuture(result);
            }
        }

        DownloadResult fallback = new DownloadResult(index, false, 0, 0, null,
                "Retry loop exited unexpectedly", FailureType.UNKNOWN, maxRetries);
        tracker.recordResult(jobId, fallback, threadName);
        return CompletableFuture.completedFuture(fallback);
    }

    private DownloadResult tryDownload(String urlString, int index, long startNs, int retryCount, String threadName) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes("image/*"));
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                urlString,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        MediaType contentType = response.getHeaders().getContentType();
        byte[] imageBytes = response.getBody();
        long millis = (System.nanoTime() - startNs) / 1_000_000;

        // Chi ghi file khi response that su la anh.
        if (imageBytes != null && imageBytes.length > 0 && contentType != null && "image".equals(contentType.getType())) {
            String ext = contentType.getSubtype().equalsIgnoreCase("jpeg") ? "jpg" : contentType.getSubtype();
            Path path = Paths.get(SAVE_DIR + "image_" + index + "." + ext);
            Files.createDirectories(path.getParent());
            Files.write(path, imageBytes);

            System.out.println("Thread " + threadName +
                    " -> Saved image " + index + " (" + imageBytes.length + " bytes, " + contentType + ", " + millis + " ms, retries=" + retryCount + ")");

            return new DownloadResult(index, true, imageBytes.length, millis, contentType.toString(), null, null, retryCount);
        }

        String ct = contentType == null ? "null" : contentType.toString();
        String err = "Not an image: contentType=" + ct + ", bytes=" + (imageBytes == null ? 0 : imageBytes.length);
        throw new IllegalArgumentException(err);
    }

    private RestTemplate createRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }

    private FailureType classifyFailure(Exception exception) {
        if (exception instanceof HttpStatusCodeException) {
            return FailureType.HTTP_ERROR;
        }

        if (exception instanceof ResourceAccessException resourceAccessException) {
            Throwable rootCause = resourceAccessException.getRootCause();
            if (rootCause instanceof SocketTimeoutException timeoutException) {
                String message = timeoutException.getMessage();
                if (message != null && message.toLowerCase().contains("connect")) {
                    return FailureType.CONNECT_TIMEOUT;
                }
                return FailureType.READ_TIMEOUT;
            }
            if (rootCause instanceof ConnectException) {
                return FailureType.CONNECT_TIMEOUT;
            }
            if (rootCause instanceof IOException) {
                return FailureType.NETWORK_ERROR;
            }
        }

        if (exception instanceof IllegalArgumentException) {
            return FailureType.INVALID_RESPONSE;
        }

        if (exception instanceof java.nio.file.FileSystemException || exception instanceof IOException) {
            return FailureType.IO_WRITE_ERROR;
        }

        return FailureType.UNKNOWN;
    }

    private boolean shouldRetry(FailureType failureType) {
        return failureType == FailureType.CONNECT_TIMEOUT
                || failureType == FailureType.READ_TIMEOUT
                || failureType == FailureType.NETWORK_ERROR;
    }

    private void sleepBeforeRetry() {
        if (retryBackoffMs <= 0) {
            return;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(retryBackoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
