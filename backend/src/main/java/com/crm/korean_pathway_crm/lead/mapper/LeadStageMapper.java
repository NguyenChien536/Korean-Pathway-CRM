package com.crm.korean_pathway_crm.lead.mapper;

import com.crm.korean_pathway_crm.lead.dto.response.LeadStageResponse;
import com.crm.korean_pathway_crm.pipeline.entity.PipelineStage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LeadStageMapper {
    LeadStageResponse toResponse(PipelineStage stage);
}
