package com.crm.korean_pathway_crm.lead.service;

import com.crm.korean_pathway_crm.common.enums.CatalogStatus;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.entity.LeadSource;
import com.crm.korean_pathway_crm.lead.repository.LeadSourceRepository;
import com.crm.korean_pathway_crm.pipeline.entity.Pipeline;
import com.crm.korean_pathway_crm.pipeline.entity.PipelineStage;
import com.crm.korean_pathway_crm.pipeline.enums.PipelineEntityType;
import com.crm.korean_pathway_crm.pipeline.enums.PipelineStageCategory;
import com.crm.korean_pathway_crm.pipeline.repository.PipelineRepository;
import com.crm.korean_pathway_crm.pipeline.repository.PipelineStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadCatalogService {
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository pipelineStageRepository;
    private final LeadSourceRepository leadSourceRepository;

    @Transactional(readOnly = true)
    public InitialLeadStage resolveInitialLeadStage() {
        Pipeline pipeline = pipelineRepository.findByEntityTypeAndDefaultPipelineTrueAndStatus(
                        PipelineEntityType.LEAD, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_PIPELINE_NOT_CONFIGURED));

        PipelineStage stage = pipelineStageRepository
                .findByPipelineIdAndInitialTrueAndTerminalFalseAndCategoryAndStatus(
                        pipeline.getId(), PipelineStageCategory.OPEN, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_INITIAL_STAGE_NOT_CONFIGURED));

        return new InitialLeadStage(pipeline.getId(), stage.getId());
    }

    @Transactional(readOnly = true)
    public UUID resolveActiveSourceId(UUID sourceId) {
        if (sourceId == null) {
            return null;
        }
        return leadSourceRepository.findByIdAndStatus(sourceId, CatalogStatus.ACTIVE)
                .map(LeadSource::getId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_SOURCE_NOT_FOUND));
    }

    public record InitialLeadStage(UUID pipelineId, UUID stageId) {
    }
}
