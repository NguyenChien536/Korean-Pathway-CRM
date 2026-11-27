package com.crm.korean_pathway_crm.lead.repository;

import com.crm.korean_pathway_crm.lead.entity.LeadStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadStatusHistoryRepository extends JpaRepository<LeadStatusHistory, UUID> {
}
