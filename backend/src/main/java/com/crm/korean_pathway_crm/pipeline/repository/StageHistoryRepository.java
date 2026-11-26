package com.crm.korean_pathway_crm.pipeline.repository;

import com.crm.korean_pathway_crm.pipeline.entity.StageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StageHistoryRepository extends JpaRepository<StageHistory, UUID> {
}
