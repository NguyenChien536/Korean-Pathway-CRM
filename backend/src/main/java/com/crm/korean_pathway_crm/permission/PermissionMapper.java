package com.crm.korean_pathway_crm.permission;

import com.crm.korean_pathway_crm.permission.dto.PermissionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toResponse(Permission permission);
}
