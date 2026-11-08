package com.crm.korean_pathway_crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntrospectRequest {
    @NotBlank(message = "Token là bắt buộc")
    String token;
}
