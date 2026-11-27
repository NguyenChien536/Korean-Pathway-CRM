package com.crm.korean_pathway_crm.lead.dto.response;

import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeadStatusResponse {
    UUID id;
    UUID leadId;
    LeadStatus fromStatus;
    LeadStatus toStatus;
    UUID changedBy;
    LocalDateTime changedAt;
    String reason;
}
