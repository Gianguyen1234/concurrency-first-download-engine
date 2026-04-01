# Failure Summary

Tài liệu này giải thích endpoint:

```http
GET /jobs/{jobId}/failure-summary
```

Endpoint này dùng để nhìn nhanh tình hình lỗi và retry của một job.

## Mục tiêu

Khi một job có nhiều task, đôi lúc xem toàn bộ `tasks` sẽ khá dài.

Failure summary giúp trả lời nhanh các câu hỏi như:

- job này có bao nhiêu task thất bại?
- có bao nhiêu task đã phải retry?
- có bao nhiêu task thành công sau retry?
- lỗi đang tập trung ở loại nào?

## Ví dụ response

```json
{
  "jobId": 1001,
  "totalTasks": 10,
  "failedTasks": 1,
  "retriedTasks": 2,
  "successfulAfterRetry": 1,
  "failureCounts": {
    "READ_TIMEOUT": 1
  }
}
```

## Ý nghĩa từng field

### `jobId`

Job đang được thống kê.

### `totalTasks`

Tổng số task thuộc job này.

### `failedTasks`

Số task có trạng thái cuối cùng là `FAILED`.

### `retriedTasks`

Số task đã từng retry ít nhất một lần.

Điều này bao gồm cả:

- task retry xong rồi thành công
- task retry xong vẫn thất bại

### `successfulAfterRetry`

Số task có `retryCount > 0` và trạng thái cuối cùng là `SUCCESS`.

Đây là chỉ số rất hay để biết retry policy có đang giúp ích không.

### `failureCounts`

Bảng đếm số lỗi cuối cùng theo từng `failureType`.

Ví dụ:

```json
{
  "READ_TIMEOUT": 1,
  "CONNECT_TIMEOUT": 2
}
```

nghĩa là:

- có 1 task cuối cùng thất bại vì `READ_TIMEOUT`
- có 2 task cuối cùng thất bại vì `CONNECT_TIMEOUT`

## Lưu ý quan trọng

`failureCounts` chỉ tính các task có trạng thái cuối cùng là `FAILED`.

Nghĩa là nếu một task:

- lần đầu bị `READ_TIMEOUT`
- sau đó retry thành công

thì task đó:

- được tính vào `retriedTasks`
- có thể được tính vào `successfulAfterRetry`
- nhưng **không** được tính vào `failedTasks`
- và **không** xuất hiện trong `failureCounts`

## Khi nào nên dùng endpoint này?

Nên dùng khi bạn muốn:

- nhìn nhanh reliability của một job
- so sánh job giữa các lần chạy
- chuẩn bị benchmark/report
- kiểm tra retry policy có đang cứu được bao nhiêu task