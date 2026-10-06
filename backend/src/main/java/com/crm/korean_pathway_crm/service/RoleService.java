package com.crm.korean_pathway_crm.service;

import com.crm.korean_pathway_crm.dto.RoleDto;
import com.crm.korean_pathway_crm.dto.RoleRequest;
import java.util.List;
import java.util.UUID;

public interface RoleService {
    RoleDto createRole(RoleRequest request);
    RoleDto updateRole(UUID id, RoleRequest request);
    RoleDto getRoleById(UUID id);
    List<RoleDto> getAllRoles();
    void deleteRole(UUID id);
}
