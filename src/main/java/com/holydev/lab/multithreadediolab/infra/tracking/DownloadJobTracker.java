package com.holydev.lab.multithreadediolab.infra.tracking;

import com.holydev.lab.multithreadediolab.domain.download.DownloadResult;
import com.holydev.lab.multithreadediolab.domain.download.FailureType;
import com.holydev.lab.multithreadediolab.domain.job.CancelJobResponse;
import com.holydev.lab.multithreadediolab.domain.job.DownloadJobSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.DownloadTaskSnapshot;
import com.holydev.lab.multithreadediolab.domain.job.JobFailureSummary;
import com.holydev.lab.multithreadediolab.domain.job.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DownloadJobTracker {

    private final AtomicLong jobIdSequence = new AtomicLong(1000);
    // Lưu runtime state của nhiều job trong memory.
    private final Map<Long, JobRuntimeState> jobs = new ConcurrentHashMap<>();

    // Tạo 1 job mới và cấp jobId tăng dần để tiện test/demo.
    public long startJob(String sourceBaseUrl, int totalRequested) {
        long jobId = jobIdSequence.incrementAndGet();
        JobRuntimeState state = new JobRuntimeState(jobId, sourceBaseUrl, totalRequested);
        jobs.put(jobId, state);
        return jobId;
    }

    public void registerTask(long jobId, int index, String url) {
        JobRuntimeState state = jobs.get(jobId);
        if (state != null) {
            state.registerTask(index, url);
        }
    }

    public boolean markTaskRunning(long jobId, int index, String threadName) {
        JobRuntimeState state = jobs.get(jobId);
        if (state != null) {
            return state.markTaskRunning(index, threadName);
        }
        return false;
    }

    public void markTaskRetry(long jobId, int index, int retryCount, String threadName, FailureType failureType, String error) {
        JobRuntimeState state = jobs.get(jobId);
        if (state != null) {
            state.markTaskRetry(index, retryCount, threadName, failureType, error);
        }
    }

    public void recordResult(long jobId, DownloadResult result, String threadName) {
        JobRuntimeState state = jobs.get(jobId);
        if (state != null) {
            state.recordResult(result, threadName);
        }
    }

    public DownloadJobSnapshot getJob(long jobId) {
        JobRuntimeState state = jobs.get(jobId);
        return state == null ? null : state.snapshot();
    }

    public List<DownloadTaskSnapshot> getTasks(long jobId) {
        JobRuntimeState state = jobs.get(jobId);
        return state == null ? List.of() : state.taskSnapshots();
    }

    public JobFailureSummary getFailureSummary(long jobId) {
        JobRuntimeState state = jobs.get(jobId);
        return state == null ? null : state.failureSummary();
    }

    public CancelJobResponse cancelJob(long jobId) {
        JobRuntimeState state = jobs.get(jobId);
        return state == null ? null : state.cancel();
    }

    public boolean isJobCancelled(long jobId) {
        JobRuntimeState state = jobs.get(jobId);
        return state != null && state.isCancelled();
    }

    // State nội bộ của 1 job. Dùng synchronized vì nhiều worker thread có thể update cùng lúc.
    private static final class JobRuntimeState {
        private final long jobId;
        private final String sourceBaseUrl;
        private final int totalRequested;
        private final long startedAtNs;
        private final Map<Integer, TaskRuntimeState> tasks = new LinkedHashMap<>();

        private int queued;
        private int running;
        private int completed;
        private int okCount;
        private int failCount;
        private int cancelledCount;
        private long totalBytes;
        private long totalTaskMillis;
        private long finishedAtNs;
        private boolean cancelled;

        private JobRuntimeState(long jobId, String sourceBaseUrl, int totalRequested) {
            this.jobId = jobId;
            this.sourceBaseUrl = sourceBaseUrl;
            this.totalRequested = totalRequested;
            this.startedAtNs = System.nanoTime();
        }

        // Khi job vừa tạo, từng task được đưa vào trạng thái QUEUED trước.
        private synchronized void registerTask(int index, String url) {
            tasks.put(index, new TaskRuntimeState(index, url));
            queued++;
        }

        // Worker lấy được task thì task chuyển từ QUEUED sang RUNNING.
        private synchronized boolean markTaskRunning(int index, String threadName) {
            TaskRuntimeState task = tasks.get(index);
            if (task == null || task.status != TaskStatus.QUEUED) {
                return false;
            }

            if (cancelled) {
                task.status = TaskStatus.CANCELLED;
                task.error = "Job was cancelled before the task started";
                queued--;
                completed++;
                cancelledCount++;
                finishIfDone();
                return false;
            }

            task.status = TaskStatus.RUNNING;
            task.threadName = threadName;
            queued--;
            running++;
            return true;
        }

        // Khi task fail nhưng vẫn còn quyền retry, ta cập nhật số lần retry gần nhất để client nhìn thấy tiến trình.
        private synchronized void markTaskRetry(int index, int retryCount, String threadName, FailureType failureType, String error) {
            TaskRuntimeState task = tasks.get(index);
            if (task == null) {
                return;
            }

            task.retryCount = retryCount;
            task.threadName = threadName;
            task.failureType = failureType;
            task.error = error;
        }

        // Mọi đường đi cuối cùng đều đổ về đây: success hay fail đều tính là task đã hoàn tất.
        private synchronized void recordResult(DownloadResult result, String threadName) {
            TaskRuntimeState task = tasks.get(result.index());
            if (task == null) {
                return;
            }

            if (task.status == TaskStatus.RUNNING) {
                running--;
            } else if (task.status == TaskStatus.CANCELLED) {
                return;
            } else if (task.status == TaskStatus.QUEUED) {
                queued--;
            }

            completed++;
            totalTaskMillis += result.millis();

            task.status = result.ok() ? TaskStatus.SUCCESS : TaskStatus.FAILED;
            task.bytes = result.bytes();
            task.millis = result.millis();
            task.retryCount = result.retryCount();
            task.contentType = result.contentType();
            task.error = result.error();
            task.failureType = result.failureType();
            task.threadName = threadName;

            if (result.ok()) {
                okCount++;
                totalBytes += result.bytes();
            } else {
                failCount++;
            }

            finishIfDone();
        }

        // Snapshot này là thứ controller trả ra cho client.
        private synchronized DownloadJobSnapshot snapshot() {
            long endAtNs = finishedAtNs != 0 ? finishedAtNs : System.nanoTime();
            long totalWallMillis = (endAtNs - startedAtNs) / 1_000_000;
            double totalSeconds = totalWallMillis / 1000.0;
            double avgTaskMillis = completed == 0 ? 0 : (double) totalTaskMillis / completed;
            double throughputImagesPerSecond = totalSeconds == 0 ? 0 : okCount / totalSeconds;
            double throughputMegabytesPerSecond = totalSeconds == 0 ? 0 : (totalBytes / (1024.0 * 1024.0)) / totalSeconds;
            boolean finished = totalRequested > 0 && completed >= totalRequested;

            return new DownloadJobSnapshot(
                    jobId,
                    sourceBaseUrl,
                    totalRequested,
                    queued,
                    running,
                    completed,
                    okCount,
                    failCount,
                    cancelledCount,
                    totalBytes,
                    totalWallMillis,
                    avgTaskMillis,
                    throughputImagesPerSecond,
                    throughputMegabytesPerSecond,
                    cancelled,
                    finished
            );
        }

        // Trả danh sách task theo index để JSON dễ đọc hơn.
        private synchronized List<DownloadTaskSnapshot> taskSnapshots() {
            List<DownloadTaskSnapshot> snapshots = new ArrayList<>();
            tasks.values().stream()
                    .sorted(Comparator.comparingInt(task -> task.index))
                    .forEach(task -> snapshots.add(task.snapshot()));
            return snapshots;
        }

        private synchronized JobFailureSummary failureSummary() {
            Map<FailureType, Integer> failureCounts = new EnumMap<>(FailureType.class);
            int failedTasks = 0;
            int retriedTasks = 0;
            int successfulAfterRetry = 0;

            for (TaskRuntimeState task : tasks.values()) {
                if (task.retryCount > 0) {
                    retriedTasks++;
                    if (task.status == TaskStatus.SUCCESS) {
                        successfulAfterRetry++;
                    }
                }

                if (task.status == TaskStatus.FAILED) {
                    failedTasks++;
                    FailureType failureType = task.failureType == null ? FailureType.UNKNOWN : task.failureType;
                    failureCounts.merge(failureType, 1, Integer::sum);
                }
            }

            Map<String, Integer> failureCountsView = new LinkedHashMap<>();
            for (Map.Entry<FailureType, Integer> entry : failureCounts.entrySet()) {
                failureCountsView.put(entry.getKey().name(), entry.getValue());
            }

            return new JobFailureSummary(
                    jobId,
                    tasks.size(),
                    failedTasks,
                    retriedTasks,
                    successfulAfterRetry,
                    failureCountsView
            );
        }

        private synchronized CancelJobResponse cancel() {
            if (!cancelled) {
                cancelled = true;
                for (TaskRuntimeState task : tasks.values()) {
                    if (task.status == TaskStatus.QUEUED) {
                        task.status = TaskStatus.CANCELLED;
                        task.error = "Job was cancelled before the task started";
                        queued--;
                        completed++;
                        cancelledCount++;
                    }
                }
                finishIfDone();
            }

            return new CancelJobResponse(
                    jobId,
                    true,
                    cancelledCount,
                    "Job cancelled. Running tasks may finish their current attempt, but queued tasks and future retries are stopped."
            );
        }

        private synchronized boolean isCancelled() {
            return cancelled;
        }

        private void finishIfDone() {
            if (finishedAtNs == 0 && totalRequested > 0 && completed >= totalRequested) {
                finishedAtNs = System.nanoTime();
            }
        }
    }

    private static final class TaskRuntimeState {
        private final int index;
        private final String url;
        // Task luôn bắt đầu ở QUEUED, sau đó mới RUNNING rồi SUCCESS/FAILED.
        private TaskStatus status = TaskStatus.QUEUED;
        private long bytes;
        private long millis;
        private int retryCount;
        private String contentType;
        private String error;
        private FailureType failureType;
        private String threadName;

        private TaskRuntimeState(int index, String url) {
            this.index = index;
            this.url = url;
        }

        private DownloadTaskSnapshot snapshot() {
            return new DownloadTaskSnapshot(index, url, status, bytes, millis, retryCount, contentType, error, failureType, threadName);
        }
    }
}
