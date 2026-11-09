package com.crm.korean_pathway_crm.permission;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    List<RolePermission> findAllByRole_Id(UUID roleId);

    @Query("""
        select p
        from RolePermission rp
        join rp.permission p
        where rp.role.id = :roleId
        order by p.module asc, p.code asc
        """)
    List<Permission> findPermissionsByRoleId(
            @Param("roleId") UUID roleId
    );
}
