package com.crm.korean_pathway_crm.lead.service;

import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.person.dto.request.PersonCreationRequest;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.entity.PersonContact;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import com.crm.korean_pathway_crm.person.service.PersonContactService;
import com.crm.korean_pathway_crm.person.service.PersonService;
import com.crm.korean_pathway_crm.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {
    private final LeadRepository leadRepository;
    private final LeadCatalogService leadCatalogService;
    private final PersonService personService;
    private final PersonContactService personContactService;
    private final CurrentActorService currentActorService;

    @Transactional
    @PreAuthorize("hasAuthority('LEADS.CREATE')")
    public LeadResponse createLead(LeadCreationRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        User actor = currentActorService.requireCurrentActor();
        String fullName = normalizeRequiredName(request.getFullName());
        String nameForMatch = normalizeNameForMatch(fullName);

        // Tìm các Person có cùng số. Person đã xóa mềm không tham gia so khớp.
        List<PersonContact> contactsWithSamePhone = personContactService.findByPhone(request.getPhone());
        boolean sharedPhone = false;
        for (PersonContact contact : contactsWithSamePhone) {
            Person existing = contact.getPerson();
            if (existing.getDeletedAt() != null) {
                continue;
            }
            sharedPhone = true;
            if (normalizeNameForMatch(existing.getFullName()).equals(nameForMatch)) {
                // Chưa có bước xác nhận dùng Person cũ hay tạo người mới.
                // Không tự gộp người và không trả ID của Person khác cho counselor.
                throw new AppException(ErrorCode.PERSON_CONFIRMATION_REQUIRED);
            }
        }

        // Tra cứu cấu hình bằng code/trạng thái, không ghi cứng UUID trong Java.
        LeadCatalogService.InitialLeadStage initialStage = leadCatalogService.resolveInitialLeadStage();
        UUID sourceId = leadCatalogService.resolveActiveSourceId(request.getSourceId());

        // Cả Person, Contact và Lead cùng nằm trong transaction này.
        Person person = personService.create(
                PersonCreationRequest.builder().fullName(fullName).build(), actor);
        PersonContact phone = personContactService.createPhone(person, request.getPhone());

        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            email = email.strip();
            personContactService.create(person, PersonContactType.EMAIL,
                    email, email.toLowerCase(Locale.ROOT));
        } else {
            email = null;
        }

        Lead lead = Lead.builder()
                .personId(person.getId())
                .sourceId(sourceId)
                .pipelineId(initialStage.pipelineId())
                .stageId(initialStage.stageId())
                .assignedTo(actor.getId())
                .status(LeadStatus.OPEN)
                .campaignName(request.getCampaignName() == null ? null : request.getCampaignName().strip())
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        lead = leadRepository.save(lead);

        return LeadResponse.builder()
                .id(lead.getId())
                .personId(person.getId())
                .fullName(person.getFullName())
                .phone(phone.getNormalizedValue())
                .email(email)
                .sourceId(lead.getSourceId())
                .pipelineId(lead.getPipelineId())
                .stageId(lead.getStageId())
                .assignedTo(lead.getAssignedTo())
                .status(lead.getStatus())
                .campaignName(lead.getCampaignName())
                .currencyCode(lead.getCurrencyCode())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .sharedPhoneWarning(sharedPhone)
                .build();
    }

    private String normalizeRequiredName(String value) {
        if (value == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        String normalized = value.replaceAll("(?U)\\s+", " ").strip();
        if (normalized.length() < 2 || normalized.length() > 200) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private String normalizeNameForMatch(String fullName) {
        return normalizeRequiredName(fullName).toLowerCase(Locale.ROOT);
    }
}
