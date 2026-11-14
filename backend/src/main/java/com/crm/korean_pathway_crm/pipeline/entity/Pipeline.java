package com.crm.korean_pathway_crm.pipeline.entity;

import com.crm.korean_pathway_crm.pipeline.enums.PipelineEntityType;
import com.crm.korean_pathway_crm.common.enums.CatalogStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pipelines")
@Getter
@NoArgsConstructor
public class Pipeline {
    @Id
    UUID id;

    @Column(nullable = false, length = 80)
    String code;

    @Column(nullable = false, length = 150)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    PipelineEntityType entityType;

    @Column(name = "is_default", nullable = false)
    boolean defaultPipeline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CatalogStatus status;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
