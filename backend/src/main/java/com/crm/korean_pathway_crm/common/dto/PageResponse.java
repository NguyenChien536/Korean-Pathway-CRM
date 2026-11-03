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
    /** Danh sách kết quả của trang hiện tại. */
    List<T> content;

    /** Số trang hiện tại (0-indexed). */
    int page;

    /** Kích thước trang (số phần tử tối đa mỗi trang). */
    int size;

    /** Tổng số phần tử khớp với điều kiện filter. */
    long totalElements;

    /** Tổng số trang. */
    int totalPages;

    /** True nếu đây là trang đầu tiên. */
    boolean first;

    /** True nếu đây là trang cuối cùng. */
    boolean last;

    /**
     * Tạo PagedResponse từ Spring Data Page object.
     *
     * @param springPage kết quả từ repository.findAll(spec, pageable)
     * @param <T>        kiểu dữ liệu response
     * @return PagedResponse được điền đầy đủ thông tin phân trang
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
