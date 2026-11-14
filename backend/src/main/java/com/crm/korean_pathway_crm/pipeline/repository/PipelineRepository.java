package com.crm.korean_pathway_crm.pipeline.repository;

import com.crm.korean_pathway_crm.pipeline.entity.Pipeline;
import com.crm.korean_pathway_crm.pipeline.enums.PipelineEntityType;
import com.crm.korean_pathway_crm.common.enums.CatalogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {
    Optional<Pipeline> findByEntityTypeAndDefaultPipelineTrueAndStatus(
            PipelineEntityType entityType, CatalogStatus status);
}
