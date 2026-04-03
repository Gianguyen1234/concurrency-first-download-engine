# Benchmark Findings

Tài liệu này ghi lại 3 lần benchmark đầu tiên của project trên cùng một máy, với cùng endpoint ảnh:

- `count = 10`
- `count = 50`
- `count = 100`

Mục tiêu là nhìn rõ engine hiện tại đang hành xử ra sao khi workload tăng.

## Bảng số liệu

| Count | Total Wall (ms) | Avg Task (ms) | Throughput (img/s) | Fail Count | Retried Tasks | Successful After Retry |
|---|---:|---:|---:|---:|---:|---:|
| 10 | 3086 | 1211.2 | 3.24 | 0 | 0 | 0 |
| 50 | 11408 | 1326.0 | 4.38 | 0 | 1 | 1 |
| 100 | 13750 | 1262.9 | 7.27 | 0 | 2 | 2 |

## Nhận xét chính

### 1. Throughput tăng khi workload tăng

Đây là tín hiệu nổi bật nhất.

- `count=10` -> `3.24 img/s`
- `count=50` -> `4.38 img/s`
- `count=100` -> `7.27 img/s`

Điều này cho thấy thread pool đang tận dụng được độ song song của bài toán I/O-bound khá tốt.

Nói dễ hiểu:

- khi workload nhỏ, engine chưa có nhiều task để chồng lấp thời gian chờ mạng
- khi workload lớn hơn, thread pool có nhiều cơ hội giữ worker bận liên tục hơn
- kết quả là throughput tổng thể tăng lên

### 2. `avgTaskMillis` khá ổn định

- `1211.2 ms`
- `1326.0 ms`
- `1262.9 ms`

Dù workload tăng từ `10` lên `100`, thời gian trung bình của từng task không tăng vọt.

Đây là tín hiệu tốt vì nó cho thấy:

- engine chưa rơi vào trạng thái quá tải rõ rệt
- bottleneck hiện tại chưa làm latency trung bình phình mạnh lên
- workload đang tăng theo hướng mà hệ thống vẫn hấp thụ được

### 3. Retry policy có ích nhưng chưa gây nhiễu lớn

- `count=10` -> `retriedTasks = 0`
- `count=50` -> `retriedTasks = 1`
- `count=100` -> `retriedTasks = 2`

Điều này rất hợp lý với bài toán tải ảnh qua mạng:

- càng nhiều task thì càng dễ gặp vài request xấu hoặc chậm bất thường
- retry policy đang giúp cứu các lỗi tạm thời
- nhưng số lượng retry vẫn thấp, chưa làm hệ thống rối hoặc kéo fail count tăng lên

Đặc biệt ở `count=100`:

- `retriedTasks = 2`
- `successfulAfterRetry = 2`
- `failCount = 0`

Nghĩa là cả 2 task gặp vấn đề đều được cứu thành công.

## Kết luận sơ bộ

Ở workload từ `10` đến `100`, engine hiện tại đang behave giống một hệ thống **I/O-bound concurrent system** khá điển hình:

- throughput tăng khi số task tăng
- latency trung bình mỗi task vẫn tương đối ổn định
- retry policy bắt đầu phát huy tác dụng ở workload lớn hơn
- không có fail cuối trong 3 lần chạy đã đo

Nói ngắn gọn hơn:

> Engine đang tận dụng tốt thread pool cho bài toán I/O-bound, và các cơ chế reliability hiện tại như timeout + retry đang giúp hệ thống ổn định mà chưa làm tăng độ phức tạp vận hành quá nhiều.

## Ý nghĩa với roadmap

Kết quả benchmark này cho thấy project đã qua mức "demo chạy được" và bắt đầu có giá trị như một engine học tập/concurrency lab thực sự.

Các bước tiếp theo đáng cân nhắc:

- thêm cấu hình pool vào report benchmark
- thêm thông tin môi trường chạy như `availableProcessors`
- chạy nhiều vòng benchmark hơn để nhìn độ dao động
- sau này có thể so sánh blocking client hiện tại với một nhánh thử nghiệm `WebClient`