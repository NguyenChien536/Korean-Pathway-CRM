package com.crm.korean_pathway_crm.pipeline.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "stage_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "student_profile_id")
    private UUID studentProfileId;

    @Column(name = "from_stage_id")
    private UUID fromStageId;

    @Column(name = "to_stage_id", nullable = false)
    private UUID toStageId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 1000)
    private String reason;

    @PrePersist
    void onCreate() {
        changedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
