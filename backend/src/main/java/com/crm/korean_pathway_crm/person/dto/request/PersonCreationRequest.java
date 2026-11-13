package com.crm.korean_pathway_crm.person.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonCreationRequest {
    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(min = 2, max = 200, message = "Họ và tên phải từ 2 đến 200 ký tự")
    String fullName;

    // Date of birth is a calendar date, and is optional for initial lead intake.
    LocalDate dateOfBirth;

    @Size(max = 30, message = "Giới tính tối đa 30 ký tự")
    String gender;

    @Size(max = 100, message = "Quốc tịch tối đa 100 ký tự")
    String nationality;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    String notes;
}
