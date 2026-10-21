package com.crm.korean_pathway_crm.role.dto;

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
public class RoleDto {
     UUID id;
     String code;
     String name;
     String description;
     boolean isSystem;
     String status;
     OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
