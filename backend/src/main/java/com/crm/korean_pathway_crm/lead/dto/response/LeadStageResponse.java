package com.crm.korean_pathway_crm.lead.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class LeadStageResponse {
    private UUID id;
    private UUID pipelineId;
    private String code;
    private String name;
    private int orderIndex;
    private boolean initial;
    private String color;
}
