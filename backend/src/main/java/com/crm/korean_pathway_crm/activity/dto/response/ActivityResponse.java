package com.crm.korean_pathway_crm.activity.dto.response;

import com.crm.korean_pathway_crm.activity.enums.ActivityType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ActivityResponse {
    private UUID id;
    private UUID leadId;
    private UUID ownerId;
    private ActivityType activityType;
    private String subject;
    private String summary;
    private LocalDateTime occurredAt;
    private Integer durationMinutes;
    private String outcome;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
