package com.crm.korean_pathway_crm.role.controller;

import com.crm.korean_pathway_crm.permission.dto.PermissionResponse;
import com.crm.korean_pathway_crm.role.dto.RoleDto;
import com.crm.korean_pathway_crm.role.dto.RoleRequest;
import com.crm.korean_pathway_crm.role.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {
    @Mock private RoleService service;
    private MockMvc mvc;
    private final UUID roleId = UUID.randomUUID();
    private final String requestBody = """
            {"code":"CUSTOM","name":"Custom role"}
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new RoleController(service)).build();
    }

    @Test
    void createPreservesCreatedStatusAndWrapsResult() throws Exception {
        when(service.createRole(any(RoleRequest.class))).thenReturn(role());
        mvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(roleId.toString()));
    }

    @Test
    void updateWrapsUpdatedRole() throws Exception {
        when(service.updateRole(eq(roleId), any(RoleRequest.class))).thenReturn(role());
        mvc.perform(put("/api/v1/roles/{id}", roleId)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(roleId.toString()));
    }

    @Test
    void getRoleWrapsResult() throws Exception {
        when(service.getRoleById(roleId)).thenReturn(role());
        mvc.perform(get("/api/v1/roles/{id}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(roleId.toString()));
    }

    @Test
    void listWrapsRolesInResultArray() throws Exception {
        when(service.getAllRoles()).thenReturn(List.of(role()));
        mvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].id").value(roleId.toString()));
    }

    @Test
    void permissionListIsWrappedAndPreservesEmptyArray() throws Exception {
        PermissionResponse permission = PermissionResponse.builder().code("LEADS.READ").build();
        when(service.getRolePermissions(roleId)).thenReturn(List.of(permission), List.of());
        mvc.perform(get("/api/v1/roles/{id}/permissions", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].code").value("LEADS.READ"));
        mvc.perform(get("/api/v1/roles/{id}/permissions", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").isEmpty())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    void deleteReturnsOkWithSuccessEnvelope() throws Exception {
        mvc.perform(delete("/api/v1/roles/{id}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đã xóa vai trò thành công"))
                .andExpect(jsonPath("$.result").doesNotExist());
        verify(service).deleteRole(roleId);
    }

    private RoleDto role() {
        return RoleDto.builder().id(roleId).code("CUSTOM").name("Custom role").build();
    }
}
