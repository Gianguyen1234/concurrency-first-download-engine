# Current Status

Tài liệu này là điểm dừng ngắn gọn của project ở thời điểm hiện tại.

## Project đang là gì?

Project hiện tại đã vượt qua mức demo tải ảnh đơn giản.

Nó đang là một **concurrency-first download engine** ở mức học tập / lab có cấu trúc rõ ràng.

## Hiện đã có những gì?

### Phase 1

- mô hình `job/task`
- API theo `jobId`
- thread pool riêng
- async worker
- pool status
- job snapshot
- task snapshot
- docs nền

### Phase 2 đang có

- validate `baseUrl`
- HTTP timeout
- failure classification
- retry policy cho lỗi tạm thời
- failure summary
- cancel job
- benchmark mode
- benchmark findings

## Giá trị hiện tại của project

Project hiện giúp học và quan sát khá rõ các chủ đề:

- thread và thread pool
- I/O-bound behavior
- throughput vs latency
- timeout
- retry
- cancel semantics
- observability cơ bản

## Trạng thái hiện tại

- branch làm việc: `phase/2-reliability-control`
- Phase 1 đã được chốt bằng tag: `v0.1.0-phase-1`
- worktree sạch tại thời điểm ghi file này

## Nếu quay lại sau này thì nên bắt đầu từ đâu?

Nếu quay lại project, có 3 hướng hợp lý nhất:

1. làm rõ thêm docs về kiến trúc và giá trị project
2. làm benchmark/report giàu metadata hơn
3. tiếp tục reliability sâu hơn hoặc persistence

## Gợi ý thực tế

Nếu quay lại sau một thời gian dài, nên đọc lại theo thứ tự:

1. `README.md`
2. `docs/flow-overview.vi.md`
3. `docs/job-snapshot-guide.vi.md`
4. `docs/retry-policy.vi.md`
5. `docs/failure-summary.vi.md`
6. `docs/cancel-job.vi.md`
7. `docs/benchmark-findings.vi.md`

## Kết luận ngắn

Project hiện tại đã đủ tốt để dừng ở một mốc tử tế.

Nó chưa phải sản phẩm lớn hoàn chỉnh, nhưng cũng không còn là một bài demo rời rạc nữa.

Đây là một điểm dừng sạch, có cấu trúc, và có thể quay lại phát triển tiếp bất cứ lúc nào.