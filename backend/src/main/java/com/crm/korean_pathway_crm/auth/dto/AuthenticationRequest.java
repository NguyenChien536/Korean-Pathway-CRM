package com.crm.korean_pathway_crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    @NotBlank(message = "Username là bắt buộc")
    @Size(max = 100, message = "Username tối đa 100 ký tự")
    String username;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    String password;
}
