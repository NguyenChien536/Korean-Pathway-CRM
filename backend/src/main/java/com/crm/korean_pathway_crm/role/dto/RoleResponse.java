package com.crm.korean_pathway_crm.role.dto;

import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
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
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    boolean isSystem;
}
