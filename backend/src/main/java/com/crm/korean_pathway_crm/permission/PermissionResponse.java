package com.crm.korean_pathway_crm.permission;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionResponse {
    String id;
    String name;
    String code;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
