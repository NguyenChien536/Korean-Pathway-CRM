package com.crm.korean_pathway_crm.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class ChangePasswordRequest {
    @JsonAlias({"old_password", "oldPassword", "currentPassword", "current_password"})
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    @Size(max = 72, message = "Mật khẩu cũ tối đa 72 ký tự")
    String oldPassword;

    @JsonAlias({"new_password", "newPassword", "password"})
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt"
    )
    String newPassword;

    @JsonAlias({"confirm_password", "confirmPassword"})
    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    @Size(max = 72, message = "Mật khẩu xác nhận tối đa 72 ký tự")
    String confirmPassword;
}
