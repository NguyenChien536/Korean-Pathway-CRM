package com.crm.korean_pathway_crm.lead.entity;

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
@Table(name = "lead_sources")
@Getter
@NoArgsConstructor
public class LeadSource {
    @Id
    UUID id;

    @Column(nullable = false, length = 80)
    String code;

    @Column(nullable = false, length = 150)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CatalogStatus status;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
