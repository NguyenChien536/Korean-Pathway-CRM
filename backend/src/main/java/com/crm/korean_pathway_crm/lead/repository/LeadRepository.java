package com.crm.korean_pathway_crm.lead.repository;

import com.crm.korean_pathway_crm.lead.entity.Lead;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lead l where l.id = :id and l.deletedAt is null")
    Optional<Lead> findForStageUpdate(@Param("id") UUID id);

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
