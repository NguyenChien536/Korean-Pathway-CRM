package com.crm.korean_pathway_crm.role.repository;

import com.crm.korean_pathway_crm.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
    Optional<Role> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.id = :id")
    Optional<Role> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.code = :code")
    Optional<Role> findByCodeForUpdate(@Param("code") String code);
    Optional<Role> findByName(String name);
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM role_permissions rp
                JOIN permissions p ON p.id = rp.permission_id
                WHERE rp.role_id = :roleId
                  AND p.code = :permissionCode
            )
            """, nativeQuery = true)
    boolean hasPermission(@Param("roleId") UUID roleId,
                          @Param("permissionCode") String permissionCode);

    @Query(value = """
            SELECT p.code
            FROM role_permissions rp
            JOIN permissions p ON p.id = rp.permission_id
            WHERE rp.role_id = :roleId
            """, nativeQuery = true)
    java.util.Set<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);
}
