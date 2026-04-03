# Tổng Quan Luồng Chạy

Tài liệu này giải thích luồng chạy chính của project theo thứ tự:

- controller
- service điều phối
- async worker
- tracker
- snapshot trả về cho client

Mục tiêu là giúp bạn nhìn hệ thống như một câu chuyện liền mạch, thay vì chỉ thấy nhiều class rời rạc.

## Bức tranh lớn

Khi client gọi:

```http
GET /jobs/start?count=10&baseUrl=https://picsum.photos/300/300
```

thì hệ thống không tải đồng bộ ngay trên thread HTTP.

Thay vào đó, nó làm như sau:

1. Controller nhận request.
2. Service tạo một `job` mới.
3. Service sinh ra nhiều `task` con.
4. Mỗi task được đẩy sang worker async.
5. Worker tải ảnh và ghi file.
6. Tracker cập nhật trạng thái của từng task và cả job.
7. Client gọi endpoint status để xem snapshot hiện tại.

## 1. Controller làm gì?

File chính:

- `DownloadController`

Vai trò của controller là:

- nhận HTTP request
- tách input từ query params
- gọi sang service
- trả response JSON về cho client

Controller **không nên** là nơi chứa logic tải ảnh thật sự.

Ví dụ endpoint:

- `GET /jobs/start`
- `GET /jobs/{jobId}`
- `GET /jobs/{jobId}/tasks`
- `GET /jobs/{jobId}/failure-summary`
- `POST /jobs/{jobId}/cancel`
- `GET /benchmarks/run`

### Ý nghĩa từng endpoint

#### `GET /jobs/start`

Dùng để bắt đầu một job mới.

Nó trả về rất nhanh với:

- `jobId`
- `count`
- `baseUrl`
- `message`

Nó **không chờ tải xong**.

Đây là điểm rất quan trọng trong thiết kế async.

#### `GET /jobs/{jobId}`

Trả về snapshot tổng quan của job:

- còn bao nhiêu task đang queue
- bao nhiêu task đang chạy
- đã xong bao nhiêu
- thành công bao nhiêu
- thất bại bao nhiêu
- bị hủy bao nhiêu
- throughput hiện tại ra sao

#### `GET /jobs/{jobId}/tasks`

Trả về chi tiết từng task:

- URL nào đang chạy
- task nào thất bại
- lỗi gì
- failure type là gì
- retry bao nhiêu lần
- thread nào xử lý task đó
- mất bao nhiêu mili giây

#### `GET /jobs/{jobId}/failure-summary`

Trả về thống kê lỗi và retry của riêng job đó.

#### `POST /jobs/{jobId}/cancel`

Dùng để yêu cầu hủy job.

Semantics hiện tại là:

- task còn `QUEUED` sẽ thành `CANCELLED`
- task đang chạy được phép kết thúc attempt hiện tại
- retry trong tương lai bị chặn

#### `GET /benchmarks/run`

Dùng để chạy benchmark đơn giản bằng chính engine hiện tại.

Endpoint này sẽ:

- tạo một job
- chờ job hoàn tất hoặc timeout
- trả về report JSON tổng hợp

## 2. Service điều phối làm gì?

File chính:

- `DownloadJobService`

Đây là lớp orchestration, tức là lớp điều phối use case.

Nó không trực tiếp tải ảnh từng byte, mà chịu trách nhiệm tổ chức công việc.

Khi `startJob(count, baseUrl)` được gọi, service làm 3 việc:

1. Tạo một job mới trong tracker.
2. Sinh ra các task con từ `count` và `baseUrl`.
3. Gọi worker async để thực thi từng task.

Ví dụ nếu:

- `count = 3`
- `baseUrl = https://picsum.photos/300/300`

thì service sẽ sinh ra 3 URL:

- `https://picsum.photos/300/300?random=0`
- `https://picsum.photos/300/300?random=1`
- `https://picsum.photos/300/300?random=2`

Mỗi URL tương ứng với một task.

Ngoài `startJob`, service hiện còn điều phối:

- lấy snapshot job
- lấy danh sách task
- lấy failure summary
- cancel job
- chạy benchmark mode

## 3. Async worker làm gì?

File chính:

- `ImageDownloaderService`

Đây là nơi công việc thật sự được thực hiện.

Worker làm các việc sau:

1. Đánh dấu task từ `QUEUED` sang `RUNNING`.
2. Gọi HTTP tới URL cần tải.
3. Kiểm tra `Content-Type` có đúng là ảnh hay không.
4. Ghi file xuống thư mục `downloads`.
5. Tạo `DownloadResult`.
6. Báo kết quả về tracker.

### Vì sao gọi là async worker?

Vì method này được đánh dấu:

```java
@Async("imageTaskExecutor")
```

Điều đó có nghĩa là method sẽ chạy trên thread pool, không chạy trên thread HTTP request ban đầu.

Nói đơn giản:

- request web chỉ có nhiệm vụ khởi động công việc
- worker async mới là nơi thật sự đi tải ảnh

### Worker hiện đã có thêm gì?

Ngoài download cơ bản, worker hiện còn có:

- timeout HTTP
- failure classification
- retry policy cho lỗi tạm thời
- cancel-aware behavior ở mức an toàn

## 4. Thread pool tham gia ở đâu?

File chính:

- `AsyncConfig`

Ở đây ta tạo bean `imageTaskExecutor` bằng `ThreadPoolTaskExecutor`.

Thread pool có nhiệm vụ:

- giữ sẵn một nhóm worker thread
- task nào đến thì phân cho worker rảnh
- nếu chưa tới lượt thì task nằm trong queue

Các thông số như:

- `corePoolSize`
- `maxPoolSize`
- `queueCapacity`

ảnh hưởng trực tiếp đến cách task được xử lý.

### Vì sao bài này hợp với thread pool?

Vì đây là bài toán I/O-bound.

Mỗi task tải ảnh thường tốn nhiều thời gian chờ:

- chờ mạng
- chờ server
- chờ response
- chờ ghi file

Khi task A đang chờ mạng, thread pool vẫn có thể cho task B hoặc C chạy trên thread khác.

## 5. Tracker làm gì?

File chính:

- `DownloadJobTracker`

Tracker là nơi giữ runtime state trong memory.

Nó biết:

- hiện có những job nào
- mỗi job có những task nào
- task nào đang `QUEUED`
- task nào đang `RUNNING`
- task nào `SUCCESS`
- task nào `FAILED`
- task nào `CANCELLED`

### Vì sao cần tracker?

Nếu không có tracker, hệ thống chỉ biết:

- task đã chạy
- log có in ra console

Nhưng sẽ không trả lời được các câu hỏi như:

- job `1001` đang tới đâu rồi?
- task nào fail?
- task nào đang chạy?
- đã tải được bao nhiêu byte?
- throughput hiện tại là bao nhiêu?
- đã retry bao nhiêu task?
- có task nào bị cancel không?

Tracker chính là “sổ theo dõi tiến độ” của hệ thống.

### Vì sao tracker dùng `synchronized`?

Vì nhiều worker thread có thể cùng lúc cập nhật cùng một job.

Ví dụ:

- thread 1 vừa xong task 5
- thread 2 vừa xong task 6
- thread 3 vừa chuyển task 7 sang `RUNNING`

Nếu không đồng bộ truy cập state, số liệu có thể bị lệch.

## 6. Snapshot là gì?

Snapshot là dữ liệu đã được tracker tổng hợp lại để trả cho client.

Hiện tại có 3 nhóm dữ liệu đọc chính:

- `DownloadJobSnapshot`
- `DownloadTaskSnapshot`
- `JobFailureSummary`

### `DownloadJobSnapshot`

Đây là bức tranh lớn của cả job.

Nó cho biết:

- tổng task
- queued
- running
- completed
- okCount
- failCount
- cancelledCount
- tổng byte đã tải
- wall time của job
- thời gian trung bình mỗi task
- throughput hiện tại
- cancelled hay chưa
- finished hay chưa

### `DownloadTaskSnapshot`

Đây là ảnh chụp của từng task riêng lẻ.

Nó cho biết:

- index của task
- URL cụ thể
- trạng thái hiện tại
- số byte
- số mili giây
- retry count
- content type
- lỗi nếu có
- failure type
- thread xử lý

### `JobFailureSummary`

Đây là bản tổng hợp nhanh về reliability của job.

Nó cho biết:

- có bao nhiêu task fail cuối cùng
- có bao nhiêu task từng retry
- có bao nhiêu task thành công sau retry
- failure type nào đang xuất hiện nhiều nhất

## 7. Luồng hoàn chỉnh bằng lời

Hãy đọc cả hệ thống theo câu chuyện sau:

1. Client gọi `GET /jobs/start`.
2. `DownloadController` nhận request.
3. `DownloadJobService` tạo job mới.
4. `DownloadJobService` tạo nhiều task từ `count` và `baseUrl`.
5. Mỗi task được đăng ký vào `DownloadJobTracker` với trạng thái ban đầu là `QUEUED`.
6. `DownloadJobService` gọi `ImageDownloaderService.downloadImage(...)` cho từng task.
7. Vì có `@Async`, mỗi task được đẩy sang thread pool `imageTaskExecutor`.
8. Worker bắt đầu chạy, đánh dấu task thành `RUNNING`.
9. Worker gọi HTTP, nhận dữ liệu, kiểm tra ảnh, ghi file.
10. Worker tạo `DownloadResult`.
11. `DownloadJobTracker` nhận kết quả và cập nhật task thành `SUCCESS`, `FAILED` hoặc `CANCELLED`.
12. Khi client gọi `GET /jobs/{jobId}`, tracker trả snapshot tổng quan.
13. Khi client gọi `GET /jobs/{jobId}/tasks`, tracker trả danh sách snapshot chi tiết của từng task.
14. Nếu client gọi `GET /jobs/{jobId}/failure-summary`, tracker trả summary về lỗi và retry.
15. Nếu client gọi `POST /jobs/{jobId}/cancel`, tracker đánh dấu job bị hủy và chặn phần việc tương lai.

## 8. Phân biệt 4 khái niệm rất dễ bị trộn

### `job`

Một đợt công việc lớn.

Ví dụ:

- tải 50 ảnh từ một nguồn

### `task`

Một đơn vị công việc nhỏ bên trong job.

Ví dụ:

- tải ảnh số 17

### `thread`

Worker đang thực thi task.

Ví dụ:

- `IO-Lab-Thread-7`

### `executor`

Nơi quản lý nhóm worker thread.

Ví dụ:

- `imageTaskExecutor`

Cách nhớ:

- `job` = chiến dịch lớn
- `task` = việc con
- `thread` = người làm việc
- `executor` = đội người làm việc

## 9. Vì sao thiết kế này đáng giá?

Nếu chỉ muốn demo tải ảnh, bạn không nhất thiết phải có `job` và `task`.

Nhưng nếu muốn project lớn lên thành engine thật sự, thì các khái niệm này là nền tảng để sau này thêm:

- retry
- timeout policy
- cancel / pause / resume
- persistence
- metrics
- benchmark report
- source adapters

Nói ngắn gọn:

- không có `job/task` thì bạn chỉ có một đống lời gọi async rời rạc
- có `job/task` thì bạn có một workflow có thể quan sát, giải thích, và mở rộng

## 10. Cách đọc code cho đỡ rối

Bạn nên đọc code theo thứ tự này:

1. `DownloadController`
2. `DownloadJobService`
3. `ImageDownloaderService`
4. `DownloadJobTracker`
5. `AsyncConfig`

Nếu đọc theo thứ tự đó, bạn sẽ thấy rõ:

- request vào ở đâu
- ai chia việc
- ai làm việc thật
- ai ghi nhận trạng thái
- ai cấu hình thread pool