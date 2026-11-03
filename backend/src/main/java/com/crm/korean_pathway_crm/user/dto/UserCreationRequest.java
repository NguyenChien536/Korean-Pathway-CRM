package com.crm.korean_pathway_crm.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @NotBlank(message = "Username là bắt buộc")
    @Size(min = 2, max = 100, message = "Tên người dùng phải từ 2 đến 100 ký tự")
    String username;

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    String email;

    @Pattern(regexp = "^(|\\+?[0-9]{9,15})$", message = "Số điện thoại không hợp lệ")
    String phone;

    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(min = 2, max = 200, message = "Họ và tên phải từ 2 đến 200 ký tự")
    String fullName;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt"
    )
    String password;

    @NotBlank(message = "Xác nhận mật khẩu là bắt buộc")
    String confirmPassword;

    @NotNull(message = "Vai trò là bắt buộc")
    UUID roleId;



}
