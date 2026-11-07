package com.crm.korean_pathway_crm.permission;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {
    PermissionRepository permissionRepository;
    UserRepository userRepository;
    PermissionMapper permissionMapper;

    public PermissionResponse create(PermissionRequest request){
        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll(){
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        String userId = auth.getName();
        var user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isAdmin = SystemRoleCode.SYSTEM_ADMIN.name().equals(user.getRole().getName());

        List<Permission> permissions;
        if (isAdmin){
            permissions = permissionRepository.findAll();
        }else {
            permissions = List.copyOf(user.getRole().getPermissions());
        }
        return permissions.stream().map(permissionMapper::toPermissionResponse).toList();
    }

    public void delete(String permissionId){
        permissionRepository.deleteById(permissionId);
    }
}
