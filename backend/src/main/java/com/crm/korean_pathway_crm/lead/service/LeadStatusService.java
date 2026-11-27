package com.crm.korean_pathway_crm.lead.service;

import com.crm.korean_pathway_crm.common.enums.CatalogStatus;
import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadStatusUpdateRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.security.LeadAccessPolicy;
import com.crm.korean_pathway_crm.lead.entity.LeadStatusHistory;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.mapper.LeadMapper;
import com.crm.korean_pathway_crm.lead.mapper.LeadStatusMapper;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.lead.repository.LeadStatusHistoryRepository;
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
import com.crm.korean_pathway_crm.user.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeadStatusService {
    LeadStatusHistoryRepository leadStatusHistoryRepository;
    LeadRepository leadRepository;
    LeadAccessPolicy leadAccessPolicy;
    StageHistoryRepository stageHistoryRepository;
    CurrentActorService currentActorService;
    LeadStatusMapper leadStatusMapper;

    PipelineRepository pipelineRepository;
    PipelineStageRepository pipelineStageRepository;

    PersonService personService;
    PersonContactService personContactService;
    LeadMapper leadMapper;

    @Transactional
    @PreAuthorize("hasAuthority('LEADS.UPDATE')")
    public LeadResponse markLost(UUID leadId, LeadStatusUpdateRequest request) {
        if (leadId == null || request == null || request.getReason() == null
                || request.getReason().isBlank()
                || request.getReason().length() > 500) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String reason = request.getReason().strip();

        User actor = currentActorService.requireCurrentActor();
        Lead lead = leadRepository.findForStageUpdate(leadId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND));

        leadAccessPolicy.ensureAccessible(lead, actor);
        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new AppException(ErrorCode.LEAD_NOT_MODIFIABLE);
        }

        pipelineRepository.findById(lead.getPipelineId())
                .filter(pipeline ->
                        pipeline.getEntityType() == PipelineEntityType.LEAD
                                && pipeline.getStatus() == CatalogStatus.ACTIVE)
                .orElseThrow(() ->
                        new AppException(ErrorCode.LEAD_PIPELINE_UNAVAILABLE));
        // Tra stage LOST trong pipeline hiện tại; không dùng ID cố định trong code.
        PipelineStage lostStage = pipelineStageRepository
                .findByPipelineIdAndCodeAndCategoryAndTerminalTrueAndStatus(
                        lead.getPipelineId(),
                        "LOST",
                        PipelineStageCategory.LOST,
                        CatalogStatus.ACTIVE)
                .orElseThrow(() ->
                        new AppException(ErrorCode.LEAD_STAGE_UNAVAILABLE));

        // Giữ lại giá trị trước thay đổi để ghi lịch sử chính xác.
        UUID previousStageId = lead.getStageId();

        // Mapper đọc status cũ trước khi Lead được đổi sang LOST.
        LeadStatusHistory leadStatusHistory = leadStatusMapper.toLostHistory(lead, actor, reason);

        StageHistory stageHistory = StageHistory.builder()
                .pipelineId(lead.getPipelineId())
                .leadId(lead.getId())
                .fromStageId(previousStageId)
                .toStageId(lostStage.getId())
                .changedBy(actor.getId())
                .reason(reason)
                .build();

        leadStatusHistoryRepository.save(leadStatusHistory);
        stageHistoryRepository.save(stageHistory);

        lead.setStatus(LeadStatus.LOST);
        lead.setStageId(lostStage.getId());
        lead.setLostReason(reason);
        lead.setUpdatedBy(actor);

        // Flush cả Lead và hai lịch sử; transaction sẽ rollback tất cả nếu bước sau bị lỗi.
        Lead savedLead = leadRepository.saveAndFlush(lead);

        Person person = personService.getById(savedLead.getPersonId());
        String phone = personContactService.findFirstValue(person.getId(), PersonContactType.PHONE);
        String email = personContactService.findFirstValue(person.getId(), PersonContactType.EMAIL);
        return leadMapper.toLeadResponse(savedLead, person, phone, email);
    }
}
