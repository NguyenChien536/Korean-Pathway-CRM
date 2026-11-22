package com.crm.korean_pathway_crm.lead.service;

import com.crm.korean_pathway_crm.common.dto.PageResponse;
import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadAssignmentRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadFilterRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadUpdateRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.enums.PersonResolution;
import com.crm.korean_pathway_crm.lead.mapper.LeadMapper;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.lead.specification.LeadSpecification;
import com.crm.korean_pathway_crm.person.dto.request.PersonCreationRequest;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.entity.PersonContact;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import com.crm.korean_pathway_crm.person.service.PersonContactService;
import com.crm.korean_pathway_crm.person.service.PersonService;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {
    private static final Map<String, String> LEAD_SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "status", "status",
            "campaignName", "campaignName",
            "estimatedBudget", "estimatedBudget",
            "id", "id"
    );

    private final LeadRepository leadRepository;
    private final LeadCatalogService leadCatalogService;
    private final PersonService personService;
    private final PersonContactService personContactService;
    private final CurrentActorService currentActorService;
    private final LeadMapper leadMapper;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasAuthority('LEADS.CREATE')")
    public LeadResponse createLead(LeadCreationRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        PersonResolution resolution = request.getPersonResolution();
        UUID selectedPersonId = request.getExistingPersonId();
        if ((resolution == PersonResolution.USE_EXISTING_PERSON) != (selectedPersonId != null)) {
            throw new AppException(ErrorCode.PERSON_CONFIRMATION_INVALID);
        }
        User actor = currentActorService.requireCurrentActor();
        String fullName = normalizeRequiredName(request.getFullName());
        String nameForMatch = normalizeNameForMatch(fullName);

        // Tìm các Person có cùng số. Person đã xóa mềm không tham gia so khớp.
        List<PersonContact> contactsWithSamePhone = personContactService.findByPhone(request.getPhone());
        Map<UUID, PersonContact> contactsByPersonId = new LinkedHashMap<>();
        Map<UUID, PersonContact> matchingContacts = new LinkedHashMap<>();
        for (PersonContact contact : contactsWithSamePhone) {
            Person existing = contact.getPerson();
            if (existing.getDeletedAt() != null) {
                continue;
            }
            contactsByPersonId.putIfAbsent(existing.getId(), contact);
            if (normalizeNameForMatch(existing.getFullName()).equals(nameForMatch)) {
                matchingContacts.putIfAbsent(existing.getId(), contact);
            }
        }
        if (!matchingContacts.isEmpty() && resolution == null) {
            // Client tìm ứng viên trong GET /leads theo phạm vi được xem rồi gửi lựa chọn.
            throw new AppException(ErrorCode.PERSON_CONFIRMATION_REQUIRED);
        }

        // Tra cứu cấu hình bằng code/trạng thái, không ghi cứng UUID trong Java.
        LeadCatalogService.InitialLeadStage initialStage = leadCatalogService.resolveInitialLeadStage();
        UUID sourceId = leadCatalogService.resolveActiveSourceId(request.getSourceId());

        Person person;
        PersonContact phone;
        boolean sharedPhone;
        if (resolution == PersonResolution.USE_EXISTING_PERSON) {
            phone = matchingContacts.get(selectedPersonId);
            if (phone == null) {
                throw new AppException(ErrorCode.PERSON_NOT_FOUND);
            }
            person = phone.getPerson();
            List<Lead> previousLeads = leadRepository
                    .findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(person.getId());
            ensureCanReusePerson(actor, person, previousLeads);
            if (previousLeads.stream().anyMatch(lead -> lead.getStatus() == LeadStatus.OPEN)) {
                throw new AppException(ErrorCode.LEAD_ALREADY_OPEN);
            }
            if (previousLeads.stream().anyMatch(lead -> lead.getStatus() == LeadStatus.CONVERTED)) {
                throw new AppException(ErrorCode.PERSON_ALREADY_CUSTOMER);
            }
            sharedPhone = contactsByPersonId.keySet().stream().anyMatch(id -> !id.equals(person.getId()));
        } else {
            // Cả Person, Contact và Lead mới cùng nằm trong transaction này.
            person = personService.create(
                    PersonCreationRequest.builder().fullName(fullName).build(), actor);
            phone = personContactService.createPhone(person, request.getPhone());
            sharedPhone = !contactsByPersonId.isEmpty();
        }

        String email = saveOrFindEmail(person, request.getEmail());

        Lead lead = leadMapper.toLead(request, person, initialStage, sourceId, actor);
        lead.setCampaignName(normalizeCampaignName(request.getCampaignName()));
        lead = leadRepository.save(lead);

        return leadMapper.toLeadResponse(
                lead,
                person,
                phone.getNormalizedValue(),
                email,
                sharedPhone);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LEADS.READ')")
    public LeadResponse getLeadById(UUID leadId) {
        User actor = currentActorService.requireCurrentActor();
        Lead lead = leadRepository.findByIdAndDeletedAtIsNull(leadId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND));

        String roleCode = actor.getRole().getCode();
        boolean canReadAll = SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);
        if (!canReadAll && !actor.getId().equals(lead.getAssignedTo())) {
            // 404 không tiết lộ Lead của counselor khác hoặc Lead chưa được giao.
            throw new AppException(ErrorCode.LEAD_NOT_FOUND);
        }

        Person person = personService.getById(lead.getPersonId());
        String phone = firstContactValue(person.getId(), PersonContactType.PHONE);
        String email = firstContactValue(person.getId(), PersonContactType.EMAIL);

        return leadMapper.toLeadResponse(lead, person, phone, email);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('LEADS.READ')")
    public PageResponse<LeadResponse> getAllLeads(LeadFilterRequest request) {
        if (request == null || request.getPage() < 0 || request.getSize() < 1 || request.getSize() > 100) {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }
        User actor = currentActorService.requireCurrentActor();
        String roleCode = actor.getRole().getCode();

        // Phân quyền dữ liệu: counselor chỉ thấy Lead được giao.
        boolean canReadAll = SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);

        // Chỉ cho phép sort theo thuộc tính Lead đã công bố trong API.
        String sortBy = StringUtils.hasText(request.getSortBy()) ? request.getSortBy().trim() : "createdAt";
        String entityField = LEAD_SORT_FIELDS.get(sortBy);
        if (entityField == null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }
        Sort.Direction sortDir = Sort.Direction.DESC;
        if (StringUtils.hasText(request.getSortDir())) {
            try {
                sortDir = Sort.Direction.fromString(request.getSortDir().trim());
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.INVALID_PARAMETER);
            }
        }
        Sort sort = "id".equals(entityField)
                ? Sort.by(new Sort.Order(sortDir, entityField))
                : Sort.by(new Sort.Order(sortDir, entityField), Sort.Order.asc("id"));

        // Tạo Pageable (page bắt đầu từ 0).
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // Phạm vi và mọi filter đều được áp dụng trong DB trước khi đếm/phân trang.
        Page<Lead> leadPage = leadRepository.findAll(
                LeadSpecification.of(request, actor.getId(), canReadAll), pageable);

        // Tải Person và Contact một lần cho cả trang, thay vì truy vấn riêng từng Lead.
        Map<UUID, Person> personsById = new HashMap<>();
        Map<UUID, String> phonesByPersonId = new HashMap<>();
        Map<UUID, String> emailsByPersonId = new HashMap<>();
        if (!leadPage.isEmpty()) {
            List<UUID> personIds = leadPage.getContent().stream()
                    .map(Lead::getPersonId).distinct().toList();
            for (Person person : personService.findActiveByIds(personIds)) {
                personsById.put(person.getId(), person);
            }
            // Repository xếp contact chính lên trước; putIfAbsent giữ contact ưu tiên.
            for (PersonContact contact : personContactService.findPhonesAndEmailsForPersons(personIds)) {
                UUID personId = contact.getPerson().getId();
                if (contact.getContactType() == PersonContactType.PHONE) {
                    phonesByPersonId.putIfAbsent(personId, contact.getNormalizedValue());
                } else if (contact.getContactType() == PersonContactType.EMAIL) {
                    emailsByPersonId.putIfAbsent(personId, contact.getValue());
                }
            }
        }
        Page<LeadResponse> responsePage = leadPage.map(lead -> {
            Person person = personsById.get(lead.getPersonId());
            if (person == null) {
                throw new AppException(ErrorCode.PERSON_NOT_FOUND);
            }
            return leadMapper.toLeadResponse(lead, person,
                    phonesByPersonId.get(person.getId()), emailsByPersonId.get(person.getId()));
        });

        return PageResponse.of(responsePage);
    }

    @Transactional
    @PreAuthorize("hasAuthority('LEADS.UPDATE')")
    public LeadResponse updateLead(UUID leadId, LeadUpdateRequest request) {
        if (request == null || (request.getSourceId() == null
                && request.getCampaignName() == null && request.getEstimatedBudget() == null)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Lead lead = leadRepository.findByIdAndDeletedAtIsNull(leadId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND));

        // Admin/Manager sửa mọi Lead; counselor hoặc role khác chỉ sửa Lead được giao.
        User actor = currentActorService.requireCurrentActor();
        String roleCode = actor.getRole().getCode();
        boolean canUpdateAll = SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);
        if (!canUpdateAll && !actor.getId().equals(lead.getAssignedTo())) {
            // 404 không tiết lộ Lead của counselor khác hoặc Lead chưa được giao.
            throw new AppException(ErrorCode.LEAD_NOT_FOUND);
        }
        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new AppException(ErrorCode.LEAD_NOT_MODIFIABLE);
        }
        UUID validatedSourceId = null;
        if (request.getSourceId() != null) {
            validatedSourceId = leadCatalogService.resolveActiveSourceId(request.getSourceId());
        }

        // Mapper chỉ cập nhật ngân sách; sourceId và campaignName cần quy tắc nghiệp vụ riêng.
        leadMapper.updateLead(lead, request);
        if (validatedSourceId != null) {
            lead.setSourceId(validatedSourceId);
        }
        if (request.getCampaignName() != null) {
            // Chuỗi rỗng hoặc chỉ có khoảng trắng nghĩa là xóa tên chiến dịch.
            lead.setCampaignName(normalizeCampaignName(request.getCampaignName()));
        }
        lead.setUpdatedBy(actor);
        Lead savedLead = leadRepository.saveAndFlush(lead);

        Person person = personService.getById(savedLead.getPersonId());
        String phone = firstContactValue(person.getId(), PersonContactType.PHONE);
        String email = firstContactValue(person.getId(), PersonContactType.EMAIL);
        return leadMapper.toLeadResponse(savedLead, person, phone, email);
    }

    @Transactional
    @PreAuthorize("hasAuthority('LEADS.ASSIGN')")
    public LeadResponse assignLead(UUID leadId, LeadAssignmentRequest request) {
        if (leadId == null || request == null || request.getAssigneeId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Lead lead = leadRepository.findByIdAndDeletedAtIsNull(leadId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND));

        // Admin/Manager sửa mọi Lead; counselor hoặc role khác chỉ sửa Lead được giao.
        User actor = currentActorService.requireCurrentActor();
        String roleCode = actor.getRole().getCode();
        boolean canUpdateAll = SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);
        if (!canUpdateAll && !actor.getId().equals(lead.getAssignedTo())) {
            // 404 không tiết lộ Lead của counselor khác hoặc Lead chưa được giao.
            throw new AppException(ErrorCode.LEAD_NOT_FOUND);
        }
        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new AppException(ErrorCode.LEAD_NOT_MODIFIABLE);
        }

        User assignee = userRepository.findWithRoleById(request.getAssigneeId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!assignee.isActive()
                || assignee.getRole() == null
                || assignee.getRole().getStatus() != RoleStatus.ACTIVE
                || !SystemRoleCode.COUNSELOR.name().equals(assignee.getRole().getCode())) {
            throw new AppException(ErrorCode.LEAD_ASSIGNEE_INELIGIBLE);
        }
        lead.setAssignedTo(assignee.getId());

        lead.setUpdatedBy(actor);
        Lead savedLead = leadRepository.saveAndFlush(lead);

        Person person = personService.getById(savedLead.getPersonId());
        String phone = firstContactValue(person.getId(), PersonContactType.PHONE);
        String email = firstContactValue(person.getId(), PersonContactType.EMAIL);
        return leadMapper.toLeadResponse(savedLead, person, phone, email);
    }

    private void ensureCanReusePerson(User actor, Person person, List<Lead> previousLeads) {
        String roleCode = actor.getRole().getCode();
        boolean canReadAll = SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);
        boolean createdByActorWithoutLead = previousLeads.isEmpty() && person.getCreatedBy() != null
                && actor.getId().equals(person.getCreatedBy().getId());
        boolean assignedToActor = previousLeads.stream()
                .anyMatch(lead -> actor.getId().equals(lead.getAssignedTo()));
        boolean activeLeadOutsideScope = previousLeads.stream()
                .anyMatch(lead -> (lead.getStatus() == LeadStatus.OPEN
                        || lead.getStatus() == LeadStatus.CONVERTED)
                        && !actor.getId().equals(lead.getAssignedTo()));
        if (!canReadAll && (activeLeadOutsideScope || (!createdByActorWithoutLead && !assignedToActor))) {
            // Không tiết lộ Person thuộc phạm vi của counselor khác.
            throw new AppException(ErrorCode.PERSON_NOT_FOUND);
        }
    }

    private String saveOrFindEmail(Person person, String requestedEmail) {
        List<PersonContact> existingEmails = personContactService
                .findByPersonAndType(person.getId(), PersonContactType.EMAIL);
        if (!StringUtils.hasText(requestedEmail)) {
            return existingEmails.isEmpty() ? null : existingEmails.get(0).getValue();
        }
        String email = requestedEmail.strip();
        for (PersonContact contact : existingEmails) {
            if (contact.getNormalizedValue().equalsIgnoreCase(email)) {
                return contact.getValue();
            }
        }
        personContactService.create(person, PersonContactType.EMAIL,
                email, email.toLowerCase(Locale.ROOT));
        return email;
    }

    private String normalizeCampaignName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstContactValue(UUID personId, PersonContactType type) {
        return personContactService.findByPersonAndType(personId, type).stream()
                .findFirst()
                .map(contact -> type == PersonContactType.PHONE
                        ? contact.getNormalizedValue() : contact.getValue())
                .orElse(null);
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
