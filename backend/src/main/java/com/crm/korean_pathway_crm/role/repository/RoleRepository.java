package com.crm.korean_pathway_crm.role.repository;

import com.crm.korean_pathway_crm.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    boolean existsByCode(String code);
}
