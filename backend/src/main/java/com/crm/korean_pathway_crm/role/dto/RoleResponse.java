package com.crm.korean_pathway_crm.role.dto;

import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    UUID id;
    String code;
    String name;
    String description;
    RoleStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    boolean isSystem;
}
