package com.crm.korean_pathway_crm.role.dto;

import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleRequest {
    @NotBlank(message = "Code is required")
    String code;
    
    @NotBlank(message = "Name is required")
     String name;
    
     String description;
     @Builder.Default
     RoleStatus status = RoleStatus.ACTIVE;
}
