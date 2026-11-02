package com.crm.korean_pathway_crm.user.dto;

import jakarta.validation.constraints.Email;
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
public class MyInfoUpdateRequest {
    @Size(min = 2, max = 100, message = "Tên người dùng phải từ 2 đến 100 ký tự")
    @Pattern(regexp = "(?s).*\\S.*", message = "Tên người dùng không được chỉ chứa khoảng trắng")
    String username;

    @Email(message = "Email không hợp lệ")
    @Size(min = 1, max = 255, message = "Email phải từ 1 đến 255 ký tự")
    String email;

    @Size(max = 50, message = "Số điện thoại tối đa 50 ký tự")
    @Pattern(regexp = "^(|\\+?[0-9]{9,15})$", message = "Số điện thoại không hợp lệ")
    String phone;
}
