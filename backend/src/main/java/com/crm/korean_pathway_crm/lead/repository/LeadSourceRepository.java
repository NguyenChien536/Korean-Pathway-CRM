package com.crm.korean_pathway_crm.lead.repository;

import com.crm.korean_pathway_crm.lead.entity.LeadSource;
import com.crm.korean_pathway_crm.common.enums.CatalogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeadSourceRepository extends JpaRepository<LeadSource, UUID> {
    Optional<LeadSource> findByIdAndStatus(UUID id, CatalogStatus status);
}
