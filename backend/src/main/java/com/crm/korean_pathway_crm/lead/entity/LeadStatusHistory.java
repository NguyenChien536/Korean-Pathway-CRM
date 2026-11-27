package com.crm.korean_pathway_crm.lead.entity;

import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "lead_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeadStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    UUID id;
    @Column(name = "lead_id", nullable = false)
    UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    LeadStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    LeadStatus toStatus;

    @Column(name = "changed_by")
    UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    LocalDateTime changedAt;

    @Column(length = 1000)
    String reason;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (changedAt == null) {
            changedAt = now;
        }
    }
}
