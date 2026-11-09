package com.crm.korean_pathway_crm.role.controller;

import com.crm.korean_pathway_crm.permission.dto.PermissionResponse;
import com.crm.korean_pathway_crm.permission.dto.RolePermissionRequest;
import com.crm.korean_pathway_crm.role.dto.RoleDto;
import com.crm.korean_pathway_crm.role.dto.RoleRequest;
import com.crm.korean_pathway_crm.role.service.RoleService;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@RequestBody @Valid RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<RoleDto>builder()
                        .result(roleService.createRole(request))
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(@PathVariable UUID id, @RequestBody @Valid RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<RoleDto>builder()
                .result(roleService.updateRole(id, request))
                .build());
    }

    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> updateRolePermissions(
            @PathVariable UUID roleId, @Valid @RequestBody RolePermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
                .result(roleService.updateRolePermissions(roleId, request))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<RoleDto>builder()
                .result(roleService.getRoleById(id))
                .build());
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
                .result(roleService.getRolePermissions(id))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.<List<RoleDto>>builder()
                .result(roleService.getAllRoles())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã xóa vai trò thành công")
                .build());
    }
}
