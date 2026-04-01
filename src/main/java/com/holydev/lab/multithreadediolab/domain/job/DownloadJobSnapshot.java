package com.holydev.lab.multithreadediolab.domain.job;

public record DownloadJobSnapshot(
        // Mã định danh của 1 job tải.
        long jobId,

        // URL gốc mà job dùng để sinh ra các task con.
        String sourceBaseUrl,

        // Tổng số task mà job được yêu cầu xử lý ngay từ đầu.
        int totalRequested,

        // Số task còn đang nằm trong hàng đợi, chưa được worker lấy ra chạy.
        int queued,

        // Số task đang chạy tại thời điểm chụp snapshot.
        int running,

        // Số task đã kết thúc, bao gồm cả thành công lẫn thất bại.
        int completed,

        // Trong số task đã kết thúc, có bao nhiêu task thành công.
        int okCount,

        // Trong số task đã kết thúc, có bao nhiêu task thất bại.
        int failCount,

        // Trong số task của job, có bao nhiêu task đã bị hủy trước khi chạy xong.
        int cancelledCount,

        // Tổng số byte tải thành công được tính đến thời điểm hiện tại.
        long totalBytes,

        // Thời gian thực ngoài đời của job tính từ lúc start đến lúc chụp snapshot.
        long totalWallMillis,

        // Thời gian trung bình của các task đã hoàn thành.
        double avgTaskMillis,

        // Throughput theo số ảnh thành công trên mỗi giây.
        double throughputImagesPerSecond,

        // Throughput theo MB dữ liệu tải thành công trên mỗi giây.
        double throughputMegabytesPerSecond,

        // Job đã bị yêu cầu hủy hay chưa.
        boolean cancelled,

        // Job đã hoàn tất toàn bộ task hay chưa.
        boolean finished
) {
}
