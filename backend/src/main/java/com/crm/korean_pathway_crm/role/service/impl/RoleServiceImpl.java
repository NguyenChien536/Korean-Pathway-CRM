package com.crm.korean_pathway_crm.role.service.impl;

import com.crm.korean_pathway_crm.permission.PermissionMapper;
import com.crm.korean_pathway_crm.permission.Permission;
import com.crm.korean_pathway_crm.permission.PermissionRepository;
import com.crm.korean_pathway_crm.permission.RolePermission;
import com.crm.korean_pathway_crm.permission.RolePermissionId;
import com.crm.korean_pathway_crm.permission.RolePermissionRepository;
import com.crm.korean_pathway_crm.permission.dto.PermissionResponse;
import com.crm.korean_pathway_crm.permission.dto.RolePermissionRequest;
import com.crm.korean_pathway_crm.role.dto.RoleDto;
import com.crm.korean_pathway_crm.role.dto.RoleRequest;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.crm.korean_pathway_crm.role.mapper.RoleMapper;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionMapper permissionMapper;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CONFIGURATION.MANAGE')")
    public RoleDto createRole(RoleRequest request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (roleRepository.existsByCodeIgnoreCase(code)) {
            throw new AppException(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        }
        Role role = roleMapper.toEntity(request);
        role.setCode(code);
        role.setName(request.getName().trim());
        if (role.getStatus() == null) {
            role.setStatus(RoleStatus.ACTIVE);
        }
        return roleMapper.toDto(saveRole(role));
    }
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CONFIGURATION.MANAGE')")
    public RoleDto updateRole(UUID id, RoleRequest request) {
        Role role = findRole(id);
        if (!role.getCode().equalsIgnoreCase(request.getCode().trim())) {
            throw new AppException(ErrorCode.ROLE_CODE_IMMUTABLE);
        }
        if (role.isSystem() && request.getStatus() != null && request.getStatus() != RoleStatus.ACTIVE) {
            throw new AppException(ErrorCode.SYSTEM_ROLE_PROTECTED);
        }
        roleMapper.updateEntity(role, request);
        role.setName(request.getName().trim());
        return roleMapper.toDto(saveRole(role));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(UUID id) {
        Role role = findRole(id);
        return roleMapper.toDto(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CONFIGURATION.MANAGE')")
    @Override
    public List<PermissionResponse> getRolePermissions(UUID roleId) {
        // Kiểm tra role tồn tại, dùng lại helper đã có.
        findRole(roleId);

        // Lấy permission của role và chuyển từng entity thành DTO.
        return rolePermissionRepository.findPermissionsByRoleId(roleId)
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('CONFIGURATION.MANAGE')")
    public List<PermissionResponse> updateRolePermissions(UUID roleId, RolePermissionRequest request) {
        // Cùng một role chỉ được thay tập quyền bởi một transaction tại một thời điểm.
        Role role = roleRepository.findByIdForUpdate(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (request == null || request.getPermissionCodes() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Set<String> requestedCodes = new HashSet<>(request.getPermissionCodes());

        // Kiểm tra đủ mã trước khi ghi dữ liệu; [] là yêu cầu thu hồi hết quyền.
        List<Permission> requestedPermissions = requestedCodes.isEmpty()
                ? List.of() : permissionRepository.findAllByCodeIn(requestedCodes);
        Set<String> resolvedCodes = requestedPermissions.stream()
                .map(Permission::getCode).collect(Collectors.toSet());
        if (!resolvedCodes.equals(requestedCodes)) {
            throw new AppException(ErrorCode.PERMISSION_CODE_NOT_FOUND);
        }

        List<RolePermission> currentLinks = rolePermissionRepository.findAllByRole_Id(roleId);
        Set<UUID> currentIds = currentLinks.stream()
                .map(link -> link.getRolePermissionId().getPermissionId()).collect(Collectors.toSet());
        Set<UUID> requestedIds = requestedPermissions.stream()
                .map(Permission::getId).collect(Collectors.toSet());
        // Bộ quyền admin gốc do migration quản lý. PUT cùng tập hiện tại vẫn hợp lệ.
        if (SystemRoleCode.SYSTEM_ADMIN.name().equals(role.getCode()) && !currentIds.equals(requestedIds)) {
            throw new AppException(ErrorCode.SYSTEM_ADMIN_PERMISSIONS_PROTECTED);
        }

        // Giữ các liên kết còn dùng để bảo toàn created_at; chỉ xóa/thêm phần chênh lệch.
        List<RolePermission> removedLinks = currentLinks.stream()
                .filter(link -> !requestedIds.contains(link.getRolePermissionId().getPermissionId())).toList();
        List<RolePermission> addedLinks = requestedPermissions.stream()
                .filter(permission -> !currentIds.contains(permission.getId()))
                .map(permission -> {
                    RolePermission link = new RolePermission();
                    link.setRolePermissionId(new RolePermissionId(roleId, permission.getId()));
                    link.setRole(role);
                    link.setPermission(permission);
                    return link;
                }).toList();
        rolePermissionRepository.deleteAll(removedLinks);
        rolePermissionRepository.saveAll(addedLinks);
        if (!removedLinks.isEmpty() || !addedLinks.isEmpty()) {
            role.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        rolePermissionRepository.flush();

        return rolePermissionRepository.findPermissionsByRoleId(roleId).stream()
                .map(permissionMapper::toResponse).toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CONFIGURATION.MANAGE')")
    public void deleteRole(UUID id) {
        Role role = findRole(id);
        if (role.isSystem()) {
            throw new AppException(ErrorCode.SYSTEM_ROLE_PROTECTED);
        }
        if (userRepository.existsByRoleId(id)) {
            throw new AppException(ErrorCode.ROLE_IN_USE);
        }
        try {
            roleRepository.delete(role);
            roleRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.ROLE_IN_USE, exception);
        }
    }

    private Role findRole(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private Role saveRole(Role role) {
        try {
            return roleRepository.saveAndFlush(role);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof ConstraintViolationException violation
                        && "roles_code_key".equals(violation.getConstraintName())) {
                    throw new AppException(ErrorCode.ROLE_CODE_ALREADY_EXISTS, exception);
                }
                cause = cause.getCause();
            }
            throw exception;
        }
    }
}
