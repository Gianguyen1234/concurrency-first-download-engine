# Retry Policy

Tài liệu này giải thích retry policy hiện tại của download engine.

## Mục tiêu

Retry policy giúp hệ thống không bỏ cuộc quá sớm với các lỗi mạng tạm thời.

Hiện tại project chỉ retry với các lỗi có khả năng hồi phục:

- `CONNECT_TIMEOUT`
- `READ_TIMEOUT`
- `NETWORK_ERROR`

Project không retry với các lỗi không nên thử lại, ví dụ:

- `INVALID_RESPONSE`
- `HTTP_ERROR`
- `IO_WRITE_ERROR`

## Cấu hình

Các cấu hình hiện tại nằm trong `application.properties`:

```properties
app.download.max-retries=2
app.download.retry-backoff-ms=300
```

Ý nghĩa:

- `max-retries=2`: ngoài lần chạy đầu tiên, task được thử lại tối đa 2 lần.
- `retry-backoff-ms=300`: nghỉ 300ms giữa hai lần thử.

Ví dụ:

- lần đầu thất bại vì `READ_TIMEOUT`
- retry lần 1
- nếu vẫn lỗi thì retry lần 2
- nếu vẫn lỗi nữa thì task chuyển sang `FAILED`

## Dữ liệu quan sát được

`GET /jobs/{jobId}/tasks` bây giờ trả thêm:

- `failureType`: loại lỗi cuối cùng hoặc lỗi gần nhất
- `retryCount`: số lần thử lại đã thực hiện

Ví dụ:

```json
{
  "index": 0,
  "status": "FAILED",
  "millis": 11342,
  "retryCount": 2,
  "failureType": "READ_TIMEOUT"
}
```

Cách đọc:

- task số 0 đã thử lại 2 lần
- cuối cùng vẫn thất bại
- lỗi chốt là `READ_TIMEOUT`

Nếu task thành công sau retry, bạn vẫn sẽ thấy `retryCount > 0`.

## Lưu ý về `millis`

`millis` của task là thời gian của **toàn bộ vòng đời task**, không chỉ của lần thử cuối cùng.

Nghĩa là nó bao gồm:

- thời gian của các lần thử trước
- thời gian nghỉ backoff giữa các lần thử
- thời gian của lần thành công hoặc lần thất bại cuối cùng

Điều này giúp snapshot phản ánh đúng chi phí thật của task.