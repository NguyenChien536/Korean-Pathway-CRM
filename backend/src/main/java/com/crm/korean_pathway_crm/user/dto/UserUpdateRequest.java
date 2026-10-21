package com.crm.korean_pathway_crm.user.dto;

import jakarta.validation.constraints.Email;
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
public class UserUpdateRequest {
    @NotBlank(message = "Tên người dùng là bắt buộc")
    @Size(min = 2, max = 100, message = "Tên người dùng phải từ 2 đến 100 ký tự")
    String username;

    @NotBlank(message = "Email là bắt buộc")
    @Email
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    String email;

    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(min = 2, max = 200, message = "Họ và tên phải từ 2 đến 200 ký tự")
    String fullName;

    @Size(max = 50, message = "Số điện thoại tối đa 50 ký tự")
    @Pattern(regexp = "^(|\\+?[0-9]{9,15})$", message = "Số điện thoại không hợp lệ")
    String phone;
}
