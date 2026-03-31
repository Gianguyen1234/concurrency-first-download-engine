package com.holydev.lab.multithreadediolab.infra.download;

import com.holydev.lab.multithreadediolab.infra.tracking.DownloadJobTracker;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Service
public class ImageDownloaderService {
    private final DownloadJobTracker tracker;

    public record DownloadResult(int index, boolean ok, long bytes, long millis, String contentType, String error) {
    }

    // Save to project_dir/downloads
    private final String SAVE_DIR = System.getProperty("user.dir") + File.separator + "downloads" + File.separator;
    private final RestTemplate restTemplate = new RestTemplate();

    public ImageDownloaderService(DownloadJobTracker tracker) {
        this.tracker = tracker;
    }

    @Async("imageTaskExecutor")
    public CompletableFuture<DownloadResult> downloadImage(String urlString, int index, long jobId) {
        long startNs = System.nanoTime();
        String threadName = Thread.currentThread().getName();
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

            if (imageBytes != null && imageBytes.length > 0 && contentType != null && "image".equals(contentType.getType())) {
                String ext = contentType.getSubtype().equalsIgnoreCase("jpeg") ? "jpg" : contentType.getSubtype();
                Path path = Paths.get(SAVE_DIR + "image_" + index + "." + ext);
                Files.createDirectories(path.getParent());
                Files.write(path, imageBytes);

                System.out.println("Thread " + threadName +
                        " -> Saved image " + index + " (" + imageBytes.length + " bytes, " + contentType + ", " + millis + " ms)");

                DownloadResult result = new DownloadResult(index, true, imageBytes.length, millis, contentType.toString(), null);
                tracker.recordResult(jobId, result, threadName);
                return CompletableFuture.completedFuture(result);
            }

            String ct = contentType == null ? "null" : contentType.toString();
            String err = "Not an image: contentType=" + ct + ", bytes=" + (imageBytes == null ? 0 : imageBytes.length);
            System.err.println("Index " + index + " -> " + err);
            DownloadResult result = new DownloadResult(index, false, imageBytes == null ? 0 : imageBytes.length, millis, ct, err);
            tracker.recordResult(jobId, result, threadName);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            long millis = (System.nanoTime() - startNs) / 1_000_000;
            String err = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("Error at index " + index + ": " + err);
            DownloadResult result = new DownloadResult(index, false, 0, millis, null, err);
            tracker.recordResult(jobId, result, threadName);
            return CompletableFuture.completedFuture(result);
        }
    }
}
