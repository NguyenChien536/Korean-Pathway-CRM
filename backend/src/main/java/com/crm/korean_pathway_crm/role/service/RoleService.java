package com.crm.korean_pathway_crm.role.service;

import com.crm.korean_pathway_crm.permission.dto.PermissionResponse;
import com.crm.korean_pathway_crm.permission.dto.RolePermissionRequest;
import com.crm.korean_pathway_crm.role.dto.RoleDto;
import com.crm.korean_pathway_crm.role.dto.RoleRequest;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    RoleDto createRole(RoleRequest request);
    RoleDto updateRole(UUID id, RoleRequest request);
    RoleDto getRoleById(UUID id);
    List<RoleDto> getAllRoles();

    void deleteRole(UUID id);
    List<PermissionResponse> getRolePermissions(UUID roleId);
    List<PermissionResponse> updateRolePermissions(UUID roleId, RolePermissionRequest request);
}
