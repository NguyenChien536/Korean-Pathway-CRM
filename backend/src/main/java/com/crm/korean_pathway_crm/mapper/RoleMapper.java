package com.crm.korean_pathway_crm.mapper;

import com.crm.korean_pathway_crm.dto.RoleDto;
import com.crm.korean_pathway_crm.dto.RoleRequest;
import com.crm.korean_pathway_crm.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role entity);
    Role toEntity(RoleRequest request);
    void updateEntity(@MappingTarget Role entity, RoleRequest request);
}
