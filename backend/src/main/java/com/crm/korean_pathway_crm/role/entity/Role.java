package com.crm.korean_pathway_crm.role.entity;

import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
     UUID id;

    @Column(nullable = false, unique = true, length = 80)
     String code;

    @Column(nullable = false, length = 100)
     String name;

    @Column(length = 500)
     String description;

    @Column(name = "is_system", nullable = false)
     boolean isSystem;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    RoleStatus status = RoleStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
     OffsetDateTime updatedAt;
}
