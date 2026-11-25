package com.crm.korean_pathway_crm.activity.dto.request;

import com.crm.korean_pathway_crm.activity.enums.ActivityType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityCreationRequest {
    @NotNull
    private ActivityType activityType;

    @Size(max = 200)
    private String subject;

    @NotBlank
    @Size(max = 5000)
    private String summary;

    @Min(0)
    private Integer durationMinutes;

    @Size(max = 100)
    private String outcome;
}
