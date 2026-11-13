package com.crm.korean_pathway_crm.lead.dto.response;

import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeadResponse {
    UUID id;
    UUID personId;
    String fullName;
    String phone;
    String email;
    String zalo;
    UUID sourceId;
    UUID pipelineId;
    UUID stageId;
    UUID assignedTo;
    LeadStatus status;
    String campaignName;
    BigDecimal estimatedBudget;
    String currencyCode;
    String lostReason;
    LocalDateTime firstContactAt;
    LocalDateTime convertedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
