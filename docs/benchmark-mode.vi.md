# Benchmark Mode

Tài liệu này giải thích endpoint benchmark đơn giản của project:

```http
GET /benchmarks/run
```

## Mục tiêu

Endpoint này dùng chính download engine hiện tại để:

- tạo một job
- chờ job hoàn tất
- trả về report JSON cuối cùng

Nó phù hợp để so sánh nhanh các lần chạy trên:

- máy thật và máy ảo
- timeout khác nhau
- retry khác nhau
- số lượng ảnh khác nhau

## Tham số

Ví dụ:

```http
GET /benchmarks/run?count=20&baseUrl=https://picsum.photos/300/300&pollIntervalMs=200&timeoutMillis=60000
```

Ý nghĩa:

- `count`: số task cần chạy
- `baseUrl`: URL gốc để sinh task
- `pollIntervalMs`: chu kỳ poll job status trong lúc benchmark đang chờ
- `timeoutMillis`: thời gian tối đa benchmark chờ trước khi hủy job

## Cách hoạt động

1. Endpoint tạo một job mới bằng engine hiện tại.
2. Nó poll `job snapshot` theo chu kỳ `pollIntervalMs`.
3. Nếu job hoàn tất, benchmark trả report cuối cùng.
4. Nếu chờ quá `timeoutMillis`, benchmark sẽ hủy job rồi trả report có cờ `timedOut=true`.

## Ví dụ response

```json
{
  "jobId": 1001,
  "count": 20,
  "baseUrl": "https://picsum.photos/300/300",
  "pollIntervalMs": 200,
  "timeoutMillis": 60000,
  "timedOut": false,
  "cancelledOnTimeout": false,
  "jobSnapshot": {
    "jobId": 1001,
    "sourceBaseUrl": "https://picsum.photos/300/300",
    "totalRequested": 20,
    "queued": 0,
    "running": 0,
    "completed": 20,
    "okCount": 19,
    "failCount": 1,
    "cancelledCount": 0,
    "totalBytes": 245000,
    "totalWallMillis": 8234,
    "avgTaskMillis": 1800.5,
    "throughputImagesPerSecond": 2.30,
    "throughputMegabytesPerSecond": 0.03,
    "cancelled": false,
    "finished": true
  },
  "failureSummary": {
    "jobId": 1001,
    "totalTasks": 20,
    "failedTasks": 1,
    "retriedTasks": 2,
    "successfulAfterRetry": 1,
    "failureCounts": {
      "READ_TIMEOUT": 1
    }
  },
  "message": "Benchmark completed successfully."
}
```

## Cách đọc nhanh

- `timedOut=false`: benchmark chờ đủ đến lúc job xong
- `jobSnapshot`: kết quả hiệu năng cuối cùng của job
- `failureSummary`: thống kê lỗi và retry của chính job đó

Nếu `timedOut=true`:

- benchmark đã chờ quá lâu
- job đã bị hủy để dừng phần việc còn lại
- report cuối vẫn trả về snapshot và failure summary tại thời điểm kết thúc benchmark

## Gợi ý dùng thực tế

Bạn có thể dùng endpoint này để chạy cùng một bài test trên:

- máy 16 core
- máy ảo 2 core

Rồi so sánh:

- `totalWallMillis`
- `throughputImagesPerSecond`
- `failCount`
- `retriedTasks`
- `successfulAfterRetry`

Đây là cách rất hợp để học I/O-bound bằng số liệu thật thay vì chỉ cảm giác.