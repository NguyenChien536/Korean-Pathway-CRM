package com.crm.korean_pathway_crm.lead.repository;

import com.crm.korean_pathway_crm.lead.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    //Tìm Lead có đúng id và chưa bị xóa mềm
    //SELECT * FROM leads
    //WHERE id = ? AND deleted_at IS NULL;
    Optional<Lead> findByIdAndDeletedAtIsNull(UUID id);


    //Tìm mọi Lead của một Person chưa bị xóa mềm, xếp Lead mới tạo trước:
    //SELECT * FROM leads
    //WHERE person_id = ? AND deleted_at IS NULL
    //ORDER BY created_at DESC;
    List<Lead> findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID personId);
}
