package com.crm.korean_pathway_crm.pipeline.entity;

import com.crm.korean_pathway_crm.pipeline.enums.PipelineStageCategory;
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
@Table(name = "pipeline_stages")
@Getter
@NoArgsConstructor
public class PipelineStage {
    @Id
    UUID id;

    @Column(name = "pipeline_id", nullable = false)
    UUID pipelineId;

    @Column(nullable = false, length = 80)
    String code;

    @Column(nullable = false, length = 150)
    String name;

    @Column(name = "order_index", nullable = false)
    int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PipelineStageCategory category;

    @Column(name = "is_initial", nullable = false)
    boolean initial;

    @Column(name = "is_terminal", nullable = false)
    boolean terminal;

    @Column(length = 20)
    String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CatalogStatus status;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
