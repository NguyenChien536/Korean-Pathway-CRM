package com.crm.korean_pathway_crm.common.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    /** Các DTO trong trang hiện tại; không chứa entity JPA. */
    List<T> content;

    /** Chỉ số trang hiện tại, đánh số từ 0 như Spring Data. */
    int page;

    /** Kích thước trang được yêu cầu, có thể lớn hơn số phần tử thực tế ở trang cuối. */
    int size;

    /** Tổng số bản ghi khớp filter trên DB, không phải content.size(). */
    long totalElements;

    /** Tổng số trang. */
    int totalPages;

    /** True nếu trang hiện tại là trang đầu. */
    boolean first;

    /** True nếu không còn trang kết quả tiếp theo. Trang rỗng cũng được Spring xem là last. */
    boolean last;

    /**
     * Chuyển Page nội bộ của Spring thành cấu trúc JSON mà API CRM sở hữu.
     * Việc chuyển đổi này không chạy thêm truy vấn; dữ liệu đã được repository lấy.
     *
     * @param springPage kết quả từ repository.findAll(spec, pageable)
     * @param <T>        kiểu dữ liệu response
     * @return PageResponse gồm danh sách DTO và metadata phân trang
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> springPage) {
        return PageResponse.<T>builder()
                .content(springPage.getContent())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .first(springPage.isFirst())
                .last(springPage.isLast())
                .build();
    }
}
