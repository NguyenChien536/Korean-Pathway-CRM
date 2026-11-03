package com.crm.korean_pathway_crm.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String username;
    String email;
    String fullName;
    String phone;

    UUID roleId;
    String roleCode;
    String roleName;

    boolean active;
    LocalDateTime lastLoginAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    UUID createdById;
    UUID updatedById;
}
