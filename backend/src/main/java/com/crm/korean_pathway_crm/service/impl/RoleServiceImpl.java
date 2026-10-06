package com.crm.korean_pathway_crm.service.impl;

import com.crm.korean_pathway_crm.dto.RoleDto;
import com.crm.korean_pathway_crm.dto.RoleRequest;
import com.crm.korean_pathway_crm.entity.Role;
import com.crm.korean_pathway_crm.exception.ResourceNotFoundException;
import com.crm.korean_pathway_crm.mapper.RoleMapper;
import com.crm.korean_pathway_crm.repository.RoleRepository;
import com.crm.korean_pathway_crm.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleDto createRole(RoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Role code already exists");
        }
        Role role = roleMapper.toEntity(request);
        if (role.getStatus() == null) {
            role.setStatus("ACTIVE");
        }
        return roleMapper.toDto(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleDto updateRole(UUID id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        
        roleMapper.updateEntity(role, request);
        return roleMapper.toDto(roleRepository.save(role));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return roleMapper.toDto(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        roleRepository.delete(role);
    }
}
