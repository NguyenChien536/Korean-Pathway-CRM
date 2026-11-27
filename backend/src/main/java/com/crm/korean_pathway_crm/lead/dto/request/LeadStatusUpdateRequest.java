package com.crm.korean_pathway_crm.lead.dto.request;

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
public class LeadStatusUpdateRequest {
    @NotBlank
    @Size(max = 500)
    String reason;

}
