package com.crm.korean_pathway_crm.role.mapper;

import com.crm.korean_pathway_crm.role.dto.RoleDto;
import com.crm.korean_pathway_crm.role.dto.RoleRequest;
import com.crm.korean_pathway_crm.role.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role entity);
    Role toEntity(RoleRequest request);
    void updateEntity(@MappingTarget Role entity, RoleRequest request);
}
