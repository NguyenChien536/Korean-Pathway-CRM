package com.crm.korean_pathway_crm.user.entity;

import com.crm.korean_pathway_crm.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
// Cập nhật hồ sơ chỉ ghi field đã đổi, tránh ghi lại role/active từ snapshot cũ.
@org.hibernate.annotations.DynamicUpdate
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
     UUID id;

    @Column(nullable = false, length = 100)
     String username;

    @Column(nullable = false, length = 255)
     String email;

    @Column(name = "password_hash", nullable = false, length = 255)
     String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
     String fullName;

    @Column(length = 50)
     String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
     Role role;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
     boolean isActive = true;

    @Column(name = "last_login_at")
     LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
     LocalDateTime passwordChangedAt;

    @Builder.Default
    @Column(name = "failed_login_count", nullable = false)
     int failedLoginCount = 0;

    @Column(name = "locked_until")
     LocalDateTime lockedUntil;

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
}
