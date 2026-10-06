package com.crm.korean_pathway_crm.dto;

import lombok.Data;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
public class RoleDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private boolean isSystem;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
