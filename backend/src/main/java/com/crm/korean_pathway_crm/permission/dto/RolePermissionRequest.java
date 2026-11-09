package com.crm.korean_pathway_crm.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermissionRequest {
    @NotNull(message = "Danh sách quyền là bắt buộc")
    @Size(max = 100, message = "Chỉ được gửi tối đa 100 quyền mỗi lần")
    Set<
            @NotBlank(message = "Mã quyền không được để trống")
            @Size(max = 100, message = "Mã quyền tối đa 100 ký tự")
            String
    > permissionCodes;
}
