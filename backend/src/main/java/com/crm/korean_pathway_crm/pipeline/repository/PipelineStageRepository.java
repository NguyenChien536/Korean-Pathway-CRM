package com.crm.korean_pathway_crm.pipeline.repository;

import com.crm.korean_pathway_crm.pipeline.entity.PipelineStage;
import com.crm.korean_pathway_crm.pipeline.enums.PipelineStageCategory;
import com.crm.korean_pathway_crm.common.enums.CatalogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {
    Optional<PipelineStage> findByPipelineIdAndInitialTrueAndTerminalFalseAndCategoryAndStatus(
            UUID pipelineId, PipelineStageCategory category, CatalogStatus status);
}
