# Cancel Job

Tài liệu này giải thích endpoint:

```http
POST /jobs/{jobId}/cancel
```

## Mục tiêu

Endpoint này dùng để dừng một job đang chạy.

Tuy nhiên, việc cancel trong project hiện tại được làm theo cách an toàn và thực dụng:

- các task còn `QUEUED` sẽ bị chuyển sang `CANCELLED`
- các lần retry về sau sẽ bị chặn
- task đang `RUNNING` không bị ngắt cưỡng bức giữa chừng
- task đang chạy được phép kết thúc attempt hiện tại

Điều này phù hợp với cách `RestTemplate` đang chạy blocking I/O.

## Ví dụ response

```json
{
  "jobId": 1001,
  "cancelled": true,
  "cancelledTasks": 18,
  "message": "Job cancelled. Running tasks may finish their current attempt, but queued tasks and future retries are stopped."
}
```

## Ý nghĩa từng field

### `jobId`

Job vừa được yêu cầu hủy.

### `cancelled`

Cho biết hệ thống đã ghi nhận yêu cầu hủy.

### `cancelledTasks`

Số task đã bị chuyển sang `CANCELLED`.

Thông thường đây là các task trước đó còn ở trạng thái `QUEUED`.

### `message`

Giải thích ngắn về semantics hiện tại của cancel.

## Ảnh hưởng lên snapshot

Sau khi cancel:

- `GET /jobs/{jobId}` sẽ có `cancelled = true`
- `cancelledCount` sẽ tăng
- các task chưa bắt đầu sẽ có trạng thái `CANCELLED`

## Lưu ý quan trọng

Cancel job hiện tại **không cố giết thread đang chạy**.

Lý do là vì task download đang dùng blocking I/O qua `RestTemplate`.

Vì vậy semantics hiện tại là:

- hủy phần chưa chạy
- chặn retry tương lai
- cho phép phần đang chạy kết thúc attempt đang diễn ra

Đây là một bước reliability-control hợp lý trước khi đi xa hơn tới pause/resume hoặc interrupt sâu hơn.