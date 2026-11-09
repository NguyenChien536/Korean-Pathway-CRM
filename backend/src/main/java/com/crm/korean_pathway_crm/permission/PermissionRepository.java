package com.crm.korean_pathway_crm.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findAllByOrderByModuleAscCodeAsc();

    //SELECT *
    //FROM permissions
    //WHERE code IN (?, ?);
    List<Permission> findAllByCodeIn(Collection<String> codes);
}
