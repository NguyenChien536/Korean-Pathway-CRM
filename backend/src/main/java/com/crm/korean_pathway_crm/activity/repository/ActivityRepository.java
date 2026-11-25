package com.crm.korean_pathway_crm.activity.repository;

import com.crm.korean_pathway_crm.activity.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    Page<Activity> findByLeadId(UUID leadId, Pageable pageable);
}
