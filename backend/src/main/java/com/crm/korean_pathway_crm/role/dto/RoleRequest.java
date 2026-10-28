package com.crm.korean_pathway_crm.role.dto;

import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleRequest {
    @NotBlank(message = "Mã vai trò là bắt buộc")
    @Size(max = 80, message = "Mã vai trò tối đa 80 ký tự")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "Mã vai trò bắt đầu bằng chữ và chỉ chứa chữ, số, dấu gạch dưới")
    String code;

    @NotBlank(message = "Tên vai trò là bắt buộc")
    @Size(max = 100, message = "Tên vai trò tối đa 100 ký tự")
    String name;

    @Size(max = 500, message = "Mô tả vai trò tối đa 500 ký tự")
    String description;

    @Builder.Default
    RoleStatus status = RoleStatus.ACTIVE;
}
