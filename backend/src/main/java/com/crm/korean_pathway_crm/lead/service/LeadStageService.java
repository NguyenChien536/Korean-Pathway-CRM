package com.crm.korean_pathway_crm.lead.service;

import com.crm.korean_pathway_crm.common.enums.CatalogStatus;
import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadStageUpdateRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.dto.response.LeadStageResponse;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.mapper.LeadMapper;
import com.crm.korean_pathway_crm.lead.mapper.LeadStageMapper;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import com.crm.korean_pathway_crm.person.service.PersonContactService;
import com.crm.korean_pathway_crm.person.service.PersonService;
import com.crm.korean_pathway_crm.pipeline.entity.PipelineStage;
import com.crm.korean_pathway_crm.pipeline.entity.StageHistory;
import com.crm.korean_pathway_crm.pipeline.enums.PipelineEntityType;
import com.crm.korean_pathway_crm.pipeline.enums.PipelineStageCategory;
import com.crm.korean_pathway_crm.pipeline.repository.PipelineRepository;
import com.crm.korean_pathway_crm.pipeline.repository.PipelineStageRepository;
import com.crm.korean_pathway_crm.pipeline.repository.StageHistoryRepository;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadStageService {
    private final LeadRepository leadRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;
    private final StageHistoryRepository historyRepository;
    private final CurrentActorService currentActorService;
    private final PersonService personService;
    private final PersonContactService contactService;
    private final LeadMapper leadMapper;
    private final LeadStageMapper stageMapper;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LEADS.READ')")
    public List<LeadStageResponse> getAvailableStages(UUID leadId) {
        User actor = currentActorService.requireCurrentActor();
        Lead lead = leadRepository.findByIdAndDeletedAtIsNull(leadId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND));
        ensureAccessible(lead, actor);
        ensureActiveLeadPipeline(lead.getPipelineId());

        // Dùng pipeline của Lead, không dùng pipeline mặc định: cấu hình mặc định có thể thay đổi.
        return stageRepository.findByPipelineIdAndCategoryAndTerminalFalseAndStatusOrderByOrderIndexAsc(
                        lead.getPipelineId(), PipelineStageCategory.OPEN, CatalogStatus.ACTIVE)
                .stream().map(stageMapper::toResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('LEADS.UPDATE')")
    public LeadResponse updateStage(UUID leadId, LeadStageUpdateRequest request) {
        if (leadId == null || request == null || request.getStageId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        User actor = currentActorService.requireCurrentActor();
        // Khóa Lead đến cuối transaction để hai yêu cầu đổi stage không cùng ghi lịch sử từ stage cũ.
        Lead lead = leadRepository.findForStageUpdate(leadId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND));
        ensureAccessible(lead, actor);
        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new AppException(ErrorCode.LEAD_NOT_MODIFIABLE);
        }
        ensureActiveLeadPipeline(lead.getPipelineId());
        PipelineStage target = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_STAGE_UNAVAILABLE));
        if (!lead.getPipelineId().equals(target.getPipelineId())
                || target.getStatus() != CatalogStatus.ACTIVE
                || target.getCategory() != PipelineStageCategory.OPEN || target.isTerminal()) {
            // WON/LOST cần luồng chuyển Customer hoặc ghi lý do thất bại riêng.
            throw new AppException(ErrorCode.LEAD_STAGE_UNAVAILABLE);
        }

        if (!target.getId().equals(lead.getStageId())) {
            historyRepository.save(StageHistory.builder()
                    .pipelineId(lead.getPipelineId()).leadId(lead.getId())
                    .fromStageId(lead.getStageId()).toStageId(target.getId())
                    .changedBy(actor.getId()).build());
            lead.setStageId(target.getId());
            lead.setUpdatedBy(actor);
            leadRepository.saveAndFlush(lead);
        }
        // Gửi lại stage hiện tại không tạo lịch sử mới. Status của Lead vẫn là OPEN.
        Person person = personService.getById(lead.getPersonId());
        return leadMapper.toLeadResponse(lead, person,
                contactValue(person.getId(), PersonContactType.PHONE),
                contactValue(person.getId(), PersonContactType.EMAIL));
    }

    private void ensureAccessible(Lead lead, User actor) {
        String roleCode = actor.getRole().getCode();
        boolean canAccessAll = SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);
        if (!canAccessAll && !actor.getId().equals(lead.getAssignedTo())) {
            throw new AppException(ErrorCode.LEAD_NOT_FOUND);
        }
    }

    private void ensureActiveLeadPipeline(UUID pipelineId) {
        pipelineRepository.findById(pipelineId)
                .filter(pipeline -> pipeline.getEntityType() == PipelineEntityType.LEAD
                        && pipeline.getStatus() == CatalogStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_PIPELINE_UNAVAILABLE));
    }

    private String contactValue(UUID personId, PersonContactType type) {
        return contactService.findByPersonAndType(personId, type).stream().findFirst()
                .map(contact -> type == PersonContactType.PHONE
                        ? contact.getNormalizedValue() : contact.getValue())
                .orElse(null);
    }
}
