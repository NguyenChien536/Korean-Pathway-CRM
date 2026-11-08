package com.crm.korean_pathway_crm.role.service;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.permission.Permission;
import com.crm.korean_pathway_crm.permission.PermissionMapper;
import com.crm.korean_pathway_crm.permission.RolePermissionRepository;
import com.crm.korean_pathway_crm.role.dto.RoleRequest;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.mapper.RoleMapper;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.role.service.impl.RoleServiceImpl;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
    @Mock private RoleRepository roles;
    @Mock private UserRepository users;
    @Mock private RolePermissionRepository rolePermissions;
    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(roles, Mappers.getMapper(RoleMapper.class), users,
                rolePermissions, Mappers.getMapper(PermissionMapper.class));
    }

    @Test
    void createsRoleWithCanonicalCodeAndTrimmedName() {
        when(roles.saveAndFlush(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.createRole(RoleRequest.builder().code("custom_role").name(" Custom role ").build());
        assertThat(response.getCode()).isEqualTo("CUSTOM_ROLE");
        assertThat(response.getName()).isEqualTo("Custom role");
        assertThat(response.isSystem()).isFalse();
    }

    @Test
    void duplicateCodeReturnsConflictBeforeSaving() {
        when(roles.existsByCodeIgnoreCase("CUSTOM_ROLE")).thenReturn(true);
        AppException error = assertThrows(AppException.class, () -> service.createRole(
                RoleRequest.builder().code("custom_role").name("Custom role").build()));
        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        verify(roles, never()).saveAndFlush(any());
    }

    @Test
    void roleCodeCannotChangeAfterCreation() {
        Role role = existingRole(false);
        AppException error = assertThrows(AppException.class, () -> service.updateRole(role.getId(),
                RoleRequest.builder().code("OTHER").name("Other").build()));
        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ROLE_CODE_IMMUTABLE);
    }

    @Test
    void systemRoleCannotBeDisabledOrDeleted() {
        Role role = existingRole(true);
        assertThat(assertThrows(AppException.class, () -> service.deleteRole(role.getId())).getErrorCode())
                .isEqualTo(ErrorCode.SYSTEM_ROLE_PROTECTED);
        assertThat(assertThrows(AppException.class, () -> service.updateRole(role.getId(),
                RoleRequest.builder().code(role.getCode()).name("Admin").status(RoleStatus.INACTIVE).build()))
                .getErrorCode()).isEqualTo(ErrorCode.SYSTEM_ROLE_PROTECTED);
        verify(roles, never()).delete(any());
    }

    @Test
    void assignedRoleCannotBeDeleted() {
        Role role = existingRole(false);
        when(users.existsByRoleId(role.getId())).thenReturn(true);
        assertThat(assertThrows(AppException.class, () -> service.deleteRole(role.getId())).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_IN_USE);
        verify(roles, never()).delete(any());
    }

    @Test
    void previouslyInactiveSystemRoleCanBeReactivated() {
        Role role = existingRole(true);
        role.setStatus(RoleStatus.INACTIVE);
        when(roles.saveAndFlush(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.updateRole(role.getId(),
                RoleRequest.builder().code(role.getCode()).name("Admin").status(RoleStatus.ACTIVE).build());
        assertThat(response.getStatus()).isEqualTo(RoleStatus.ACTIVE.name());
    }

    @Test
    void missingRoleUsesRoleNotFoundError() {
        assertThat(assertThrows(AppException.class, () -> service.getRoleById(UUID.randomUUID())).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }

    @Test
    void missingRoleDoesNotQueryItsPermissions() {
        UUID id = UUID.randomUUID();
        assertThat(assertThrows(AppException.class, () -> service.getRolePermissions(id)).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        verifyNoInteractions(rolePermissions);
    }

    @Test
    void existingRoleWithoutPermissionsReturnsEmptyList() {
        Role role = existingRole(false);
        when(rolePermissions.findPermissionsByRoleId(role.getId())).thenReturn(List.of());

        assertThat(service.getRolePermissions(role.getId())).isEmpty();
    }

    @Test
    void assignedPermissionsAreMappedToResponseDtos() {
        Role role = existingRole(false);
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("LEADS.READ");
        permission.setName("View leads");
        permission.setModule("LEADS");
        when(rolePermissions.findPermissionsByRoleId(role.getId())).thenReturn(List.of(permission));

        var result = service.getRolePermissions(role.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(permission.getId());
        assertThat(result.get(0).getCode()).isEqualTo("LEADS.READ");
        assertThat(result.get(0).getModule()).isEqualTo("LEADS");
    }

    private Role existingRole(boolean system) {
        Role role = Role.builder().id(UUID.randomUUID()).code("CUSTOM").name("Custom").isSystem(system).build();
        when(roles.findById(role.getId())).thenReturn(Optional.of(role));
        return role;
    }
}
