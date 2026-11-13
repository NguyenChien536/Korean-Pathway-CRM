package com.crm.korean_pathway_crm.lead.entity;

import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lead {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    UUID id;

    // The service resolves and validates these IDs; the database enforces the foreign keys.
    @Column(name = "person_id", nullable = false)
    UUID personId;

    @Column(name = "source_id")
    UUID sourceId;

    @Column(name = "pipeline_id", nullable = false)
    UUID pipelineId;

    @Column(name = "stage_id", nullable = false)
    UUID stageId;

    @Column(name = "assigned_to")
    UUID assignedTo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    LeadStatus status = LeadStatus.OPEN;

    @Column(name = "campaign_name", length = 150)
    String campaignName;

    @Column(name = "estimated_budget", precision = 14, scale = 2)
    BigDecimal estimatedBudget;

    @Builder.Default
    @Column(name = "currency_code", nullable = false, length = 3, columnDefinition = "char(3)")
    String currencyCode = "VND";

    @Column(name = "lost_reason", length = 500)
    String lostReason;

    @Column(name = "first_contact_at")
    LocalDateTime firstContactAt;

    @Column(name = "converted_at")
    LocalDateTime convertedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    User updatedBy;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
