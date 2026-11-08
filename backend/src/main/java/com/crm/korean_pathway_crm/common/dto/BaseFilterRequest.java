package com.crm.korean_pathway_crm.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseFilterRequest {
    /** Từ khóa tìm kiếm; module cụ thể quyết định những field nào được tìm. */
    @Size(max = 100)
    String search;
    /** Tên sort public do client yêu cầu; service phải whitelist trước khi tạo Sort. */
    @Size(max = 50)
    String sortBy;
    /** Nhận ASC/DESC; service chuyển thành Sort.Direction và mặc định theo nghiệp vụ. */
    @Size(max = 10)
    String sortDir;

    /** Số trang bắt đầu từ 0. Nếu client bỏ qua, request dùng trang đầu. */
    @Min(0)
    int page = 0;
    /** Số phần tử tối đa của trang; giới hạn giúp kiểm soát tải truy vấn. */
    @Min(1)
    @Max(100)
    int size = 20;
}
