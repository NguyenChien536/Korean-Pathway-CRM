package com.crm.korean_pathway_crm.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @Size(min = 2, max = 100, message = "Tên người dùng phải từ 2 đến 100 ký tự")
    @Pattern(regexp = "(?s).*\\S.*", message = "Tên người dùng không được chỉ chứa khoảng trắng")
    String username;

    @Email
    @Size(min = 1, max = 255, message = "Email phải từ 1 đến 255 ký tự")
    String email;

    @Size(min = 2, max = 200, message = "Họ và tên phải từ 2 đến 200 ký tự")
    @Pattern(regexp = "(?s).*\\S.*", message = "Họ và tên không được chỉ chứa khoảng trắng")
    String fullName;

    @Size(max = 50, message = "Số điện thoại tối đa 50 ký tự")
    @Pattern(regexp = "^(|\\+?[0-9]{9,15})$", message = "Số điện thoại không hợp lệ")
    String phone;

    UUID roleId;

    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    @Pattern(
            regexp = "^(?:|(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72})$",
            message = "Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt"
    )
    String password;

    @Size(max = 72, message = "Mật khẩu xác nhận tối đa 72 ký tự")
    String confirmPassword;
}
