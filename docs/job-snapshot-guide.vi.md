# Hướng Dẫn Đọc Job Snapshot

Tài liệu này giải thích cách đọc response của `GET /jobs/{jobId}` trong project.

## Đây là ảnh chụp tại thời điểm gọi API

Snapshot của job là trạng thái tại **đúng thời điểm** bạn gọi endpoint.

Nó không nhất thiết là kết quả cuối cùng.

Ví dụ:

```json
{
  "jobId": 1001,
  "sourceBaseUrl": "https://picsum.photos/300/300",
  "totalRequested": 50,
  "queued": 0,
  "running": 8,
  "completed": 42,
  "okCount": 41,
  "failCount": 1,
  "totalBytes": 633282,
  "totalWallMillis": 22501,
  "avgTaskMillis": 2821.9523809523807,
  "throughputImagesPerSecond": 1.8221412381671924,
  "throughputMegabytesPerSecond": 0.026840797228673517,
  "finished": false
}
```

## Ý nghĩa từng thông số

### `queued`

Số task còn đang nằm trong hàng đợi, chưa được worker lấy ra chạy.

Ví dụ `queued: 0` nghĩa là:

- không còn task nào chờ trong queue của job
- các task đã được lấy ra hết để xử lý, hoặc đã xong

### `running`

Số task đang chạy tại thời điểm chụp snapshot.

Ví dụ `running: 8` nghĩa là có 8 task đang:

- chờ mạng
- nhận bytes
- ghi file
- hoặc đang ở giữa quá trình xử lý

### `completed`

Số task đã kết thúc.

Lưu ý:

- `completed` bao gồm cả `SUCCESS` và `FAILED`

Ví dụ `completed: 42` nghĩa là đã có 42 task xong việc.

### `okCount`

Số task thành công trong nhóm task đã hoàn thành.

Ví dụ `okCount: 41` nghĩa là trong 42 task đã xong, có 41 task thành công.

### `failCount`

Số task thất bại trong nhóm task đã hoàn thành.

Ví dụ `failCount: 1` nghĩa là trong 42 task đã xong, có 1 task lỗi.

### `totalBytes`

Tổng số byte tải thành công được tính đến thời điểm hiện tại.

Lưu ý:

- chỉ cộng các task `SUCCESS`
- task `FAILED` không được cộng vào đây

### `totalWallMillis`

Thời gian thực ngoài đời của job, tính từ lúc bắt đầu job đến lúc chụp snapshot.

Ví dụ:

- `22501 ms` xấp xỉ `22.5 giây`

Đây là **wall-clock time**, không phải tổng thời gian cộng dồn của từng task.

### `avgTaskMillis`

Thời gian trung bình của các task đã hoàn thành.

Công thức hiện tại:

- lấy tổng `millis` của các task đã xong
- chia cho `completed`

Ví dụ:

- `avgTaskMillis = 2821.95 ms`
- xấp xỉ `2.82 giây/task`

### `throughputImagesPerSecond`

Thông lượng theo số ảnh thành công trên mỗi giây.

Công thức gần đúng:

- `okCount / totalWallSeconds`

Ví dụ:

- `41 / 22.501 ~= 1.82 ảnh/giây`

### `throughputMegabytesPerSecond`

Thông lượng theo MB dữ liệu tải thành công trên mỗi giây.

Nó trả lời câu hỏi:

- hệ thống đang tải được bao nhiêu MB mỗi giây?

### `finished`

Cho biết job đã xong toàn bộ hay chưa.

Ví dụ `finished: false` nghĩa là:

- job vẫn chưa xong
- vì `completed < totalRequested`
- và vẫn còn task đang `running`

## Cách đọc nhanh cả cụm thông số

Với snapshot ở trên, bạn có thể đọc như sau:

- Job đã xử lý xong `42/50` task
- Trong số đó `41` thành công, `1` thất bại
- Không còn task nào chờ trong queue
- Vẫn còn `8` task đang chạy
- Đã tải được khoảng `633 KB`
- Job đã chạy khoảng `22.5 giây`
- Tốc độ hiện tại khoảng `1.82 ảnh/giây`
- Job chưa hoàn tất

## Hai công thức rất dễ nhớ

### Công thức 1

`completed = okCount + failCount`

Ví dụ:

- `42 = 41 + 1`

### Công thức 2

`totalRequested ~= queued + running + completed`

Ví dụ:

- `50 = 0 + 8 + 42`

Hai công thức này rất hữu ích để tự kiểm tra snapshot có hợp lý hay không.

## Chỗ dễ nhầm nhất: `avgTaskMillis` và `totalWallMillis`

Hai số này **không cùng nghĩa**.

### `avgTaskMillis`

Đo độ trễ trung bình của từng task đã hoàn thành.

Đây là góc nhìn theo **task riêng lẻ**.

### `totalWallMillis`

Đo thời gian thực ngoài đời của **cả job**.

Đây là góc nhìn theo **toàn bộ quá trình**.

Vì task chạy song song nên:

- wall time không bằng tổng thời gian của từng task cộng lại

Đây là dấu hiệu của concurrency.

## Vì sao `avgTaskMillis` có thể cao nhưng `throughput` vẫn khá?

Vì hai số này đo hai thứ khác nhau:

- `avgTaskMillis`: latency của từng task
- `throughput`: năng suất toàn hệ thống

## Ví dụ để hiểu

Giả sử có 3 task, mỗi task mất 3 giây.

### Nếu chạy tuần tự

- task 1: 3s
- task 2: 3s
- task 3: 3s

Tổng mất 9s.

Throughput:

- `3 ảnh / 9 giây = 0.33 ảnh/giây`

Avg task:

- vẫn là `3 giây/task`

### Nếu chạy song song

- cả 3 cùng bắt đầu
- sau 3 giây, cả 3 cùng xong

Throughput:

- `3 ảnh / 3 giây = 1 ảnh/giây`

Avg task:

- vẫn là `3 giây/task`

Kết luận:

- `avgTaskMillis` không đổi
- `throughput` tăng mạnh vì có song song

## Tại sao điều này hợp lý trong bài toán I/O-bound?

Với I/O-bound:

- task sống khá lâu
- nhưng phần lớn thời gian là đang **chờ**

Ví dụ một task download ảnh mất 2.8 giây:

- một phần nhỏ là code Java thực sự chạy
- phần lớn còn lại là:
  - chờ DNS
  - chờ kết nối TCP
  - chờ server trả response
  - chờ bytes đi qua mạng
  - chờ ghi file

Khi task A đang chờ I/O, task B, C, D vẫn có thể chạy song song trên các thread khác.

Vì vậy:

- latency của từng task có thể vẫn cao
- throughput tổng thể vẫn được cải thiện nhiều

Đó là lý do thread pool rất hợp với bài toán I/O-bound.

## Cách nói ngắn gọn 

Bạn có thể nói:

> Trong hệ thống concurrent, `avgTaskMillis` và `throughput` là hai chỉ số khác nhau. Mỗi task có thể mất vài giây vì phải chờ I/O, nhưng nhiều task chạy song song nên hệ thống vẫn hoàn thành được nhiều ảnh mỗi giây.

Hoặc ngắn gọn hơn:

> Một ảnh riêng lẻ vẫn chậm, nhưng nhiều ảnh chạy cùng lúc nên tổng năng suất của hệ thống vẫn cao.