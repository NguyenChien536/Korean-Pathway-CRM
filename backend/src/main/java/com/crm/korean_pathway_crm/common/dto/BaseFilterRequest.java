package com.crm.korean_pathway_crm.common.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseFilterRequest {
    //từ khóa tìm kiếm tự do. Ap dụng LIKE trên các field text của entiy tương ứng
    String search;
    /**
     * Tên field dùng để sắp xếp kết quả.
     * Mặc định: {@code createdAt} (mới nhất lên trước khi kết hợp với sortDir desc).
     * Danh sách field hợp lệ được kiểm tra trong từng service để tránh lỗi query.
     */
    String sortField = "creatAt";
    /**
     * Hướng sắp xếp: {@code asc} (tăng dần) hoặc {@code desc} (giảm dần).
     * Mặc định: {@code desc} (mới nhất lên trước).
     */

    String sortDir = "desc";
    String sortBy;
    //số trang
    int page = 0;
    //số phần tử mỗi trang, mặc định 20
    int size = 20;
    /**
     * Trả về kích thước trang đã được giới hạn trong khoảng [1, 200].
     * Ngăn người dùng truyền {@code size=999999} để tránh truy vấn quá tải DB.
     */
    public int getSafeSize() {
        return Math.max(1, Math.min(size, 200));
    }
}
