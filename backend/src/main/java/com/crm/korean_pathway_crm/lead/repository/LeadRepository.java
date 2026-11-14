package com.crm.korean_pathway_crm.lead.repository;

import com.crm.korean_pathway_crm.lead.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Optional<Lead> findByIdAndDeletedAtIsNull(UUID id);

    List<Lead> findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID personId);
}
