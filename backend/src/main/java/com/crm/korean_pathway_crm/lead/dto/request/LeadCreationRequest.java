package com.crm.korean_pathway_crm.lead.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class LeadCreationRequest {
    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(min = 2, max = 200, message = "Họ và tên phải từ 2 đến 200 ký tự")
    String fullName;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Số điện thoại không hợp lệ")
    String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    String email;

    @Size(max = 255, message = "Zalo tối đa 255 ký tự")
    String zalo;

    UUID sourceId;

    @Size(max = 150, message = "Tên chiến dịch tối đa 150 ký tự")
    String campaignName;
}
