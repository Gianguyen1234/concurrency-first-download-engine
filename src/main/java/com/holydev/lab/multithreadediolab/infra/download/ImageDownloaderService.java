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

@Service
public class ImageDownloaderService {
    private final DownloadJobTracker tracker;

    // Lưu file về thư mục downloads trong project để dễ kiểm tra trên máy local.
    private final String SAVE_DIR = System.getProperty("user.dir") + File.separator + "downloads" + File.separator;
    private final RestTemplate restTemplate;

    public ImageDownloaderService(
            DownloadJobTracker tracker,
            @Value("${app.download.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${app.download.read-timeout-ms:10000}") int readTimeoutMs
    ) {
        this.tracker = tracker;
        this.restTemplate = createRestTemplate(connectTimeoutMs, readTimeoutMs);
    }

    // @Async nghĩa là method này không chạy trên thread HTTP request,
    // mà chạy trên thread pool "imageTaskExecutor".
    @Async("imageTaskExecutor")
    public CompletableFuture<DownloadResult> downloadImage(String urlString, int index, long jobId) {
        long startNs = System.nanoTime();
        String threadName = Thread.currentThread().getName();

        // Vừa vào worker thì đánh dấu task từ QUEUED -> RUNNING.
        tracker.markTaskRunning(jobId, index, threadName);
        try {
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

            // Chỉ ghi file khi response thật sự là ảnh.
            if (imageBytes != null && imageBytes.length > 0 && contentType != null && "image".equals(contentType.getType())) {
                String ext = contentType.getSubtype().equalsIgnoreCase("jpeg") ? "jpg" : contentType.getSubtype();
                Path path = Paths.get(SAVE_DIR + "image_" + index + "." + ext);
                Files.createDirectories(path.getParent());
                Files.write(path, imageBytes);

                System.out.println("Thread " + threadName +
                        " -> Saved image " + index + " (" + imageBytes.length + " bytes, " + contentType + ", " + millis + " ms)");

                DownloadResult result = new DownloadResult(index, true, imageBytes.length, millis, contentType.toString(), null, null);
                // Task thành công thì ghi kết quả vào tracker để job snapshot cập nhật ngay.
                tracker.recordResult(jobId, result, threadName);
                return CompletableFuture.completedFuture(result);
            }

            String ct = contentType == null ? "null" : contentType.toString();
            String err = "Not an image: contentType=" + ct + ", bytes=" + (imageBytes == null ? 0 : imageBytes.length);
            System.err.println("Index " + index + " -> " + err);
            DownloadResult result = new DownloadResult(index, false, imageBytes == null ? 0 : imageBytes.length, millis, ct, err, FailureType.INVALID_RESPONSE);
            // Task fail vẫn phải record để job biết đã "xong" một đơn vị công việc.
            tracker.recordResult(jobId, result, threadName);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            long millis = (System.nanoTime() - startNs) / 1_000_000;
            String err = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("Error at index " + index + ": " + err);
            DownloadResult result = new DownloadResult(index, false, 0, millis, null, err, classifyFailure(e));
            // Exception được quy về một DownloadResult thất bại để pipeline không bị đứt.
            tracker.recordResult(jobId, result, threadName);
            return CompletableFuture.completedFuture(result);
        }
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
}
