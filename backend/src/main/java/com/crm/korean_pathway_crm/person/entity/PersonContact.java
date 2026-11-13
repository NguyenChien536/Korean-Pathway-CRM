package com.crm.korean_pathway_crm.person.entity;

import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "person_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonContact {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    Person person;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 30)
    PersonContactType contactType;

    @Column(name = "\"value\"", nullable = false, length = 255)
    String value;

    @Column(name = "normalized_value", nullable = false, length = 255)
    String normalizedValue;

    @Column(length = 80)
    String label;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    boolean primary = false;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    boolean verified = false;

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

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
