package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadFilterRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadUpdateRequest;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.enums.PersonResolution;
import com.crm.korean_pathway_crm.lead.mapper.LeadMapper;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.lead.service.LeadCatalogService;
import com.crm.korean_pathway_crm.lead.service.LeadService;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.entity.PersonContact;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import com.crm.korean_pathway_crm.person.service.PersonContactService;
import com.crm.korean_pathway_crm.person.service.PersonService;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {
    @Mock LeadRepository leadRepository;
    @Mock LeadCatalogService leadCatalogService;
    @Mock PersonService personService;
    @Mock PersonContactService personContactService;
    @Mock CurrentActorService currentActorService;
    // Dùng mapper thật để test cả dữ liệu do MapStruct chuyển, không mock response.
    @Spy LeadMapper leadMapper = Mappers.getMapper(LeadMapper.class);
    @InjectMocks LeadService leadService;

    @Test
    void createsLeadForNewPerson() {
        User actor = User.builder().id(UUID.randomUUID()).build();
        Person person = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        PersonContact phone = PersonContact.builder().normalizedValue("+84912345678").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678")).thenReturn(List.of());
        when(leadCatalogService.resolveInitialLeadStage())
                .thenReturn(new LeadCatalogService.InitialLeadStage(UUID.randomUUID(), UUID.randomUUID()));
        when(personService.create(any(), eq(actor))).thenReturn(person);
        when(personContactService.createPhone(person, "0912345678")).thenReturn(phone);
        when(leadRepository.save(any())).thenAnswer(invocation -> {
            Lead lead = invocation.getArgument(0);
            lead.setId(UUID.randomUUID());
            return lead;
        });

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setCampaignName("  Tuyển sinh tháng 9  ");
        var response = leadService.createLead(request);

        assertThat(response.getPersonId()).isEqualTo(person.getId());
        assertThat(response.getAssignedTo()).isEqualTo(actor.getId());
        assertThat(response.getPhone()).isEqualTo("+84912345678");
        assertThat(response.getCampaignName()).isEqualTo("Tuyển sinh tháng 9");
        assertThat(response.isSharedPhoneWarning()).isFalse();
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    void sameNameAndPhoneStopsBeforeWriting() {
        User actor = User.builder().id(UUID.randomUUID()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));

        assertThatThrownBy(() -> leadService.createLead(request("  NGUYỄN   VĂN AN  ")))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_CONFIRMATION_REQUIRED));
        verifyNoInteractions(personService, leadRepository);
    }

    @Test
    void confirmationCanCreateAnotherPersonSharingTheSamePhone() {
        User actor = User.builder().id(UUID.randomUUID()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        Person newPerson = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));
        when(leadCatalogService.resolveInitialLeadStage())
                .thenReturn(new LeadCatalogService.InitialLeadStage(UUID.randomUUID(), UUID.randomUUID()));
        when(personService.create(any(), eq(actor))).thenReturn(newPerson);
        when(personContactService.createPhone(newPerson, "0912345678"))
                .thenReturn(PersonContact.builder().normalizedValue("+84912345678").build());
        when(leadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.CREATE_NEW_PERSON);
        var response = leadService.createLead(request);

        assertThat(response.getPersonId()).isEqualTo(newPerson.getId());
        assertThat(response.isSharedPhoneWarning()).isTrue();
        verify(leadRepository, never()).findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId());
    }

    @Test
    void confirmationReusesPersonWithLostLeadWithoutReopeningIt() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("COUNSELOR").build()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An")
                .createdBy(actor).build();
        PersonContact phone = PersonContact.builder().person(existing)
                .normalizedValue("+84912345678").build();
        Lead lostLead = Lead.builder().personId(existing.getId()).assignedTo(actor.getId())
                .status(LeadStatus.LOST).build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678")).thenReturn(List.of(phone));
        when(leadRepository.findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(lostLead));
        when(leadCatalogService.resolveInitialLeadStage())
                .thenReturn(new LeadCatalogService.InitialLeadStage(UUID.randomUUID(), UUID.randomUUID()));
        when(leadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeadCreationRequest request = request("  NGUYỄN   VĂN AN  ");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(existing.getId());
        var response = leadService.createLead(request);

        assertThat(response.getPersonId()).isEqualTo(existing.getId());
        assertThat(response.getStatus()).isEqualTo(LeadStatus.OPEN);
        assertThat(lostLead.getStatus()).isEqualTo(LeadStatus.LOST);
        assertThat(response.isSharedPhoneWarning()).isFalse();
        verify(personService, never()).create(any(), any());
        verify(personContactService, never()).createPhone(any(), any());
    }

    @Test
    void reusingPersonDoesNotInsertAnEmailThatAlreadyExists() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        PersonContact phone = PersonContact.builder().person(existing)
                .normalizedValue("+84912345678").build();
        PersonContact email = PersonContact.builder().person(existing).value("an@example.com")
                .normalizedValue("an@example.com").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678")).thenReturn(List.of(phone));
        when(leadRepository.findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(Lead.builder().status(LeadStatus.LOST).build()));
        when(leadCatalogService.resolveInitialLeadStage())
                .thenReturn(new LeadCatalogService.InitialLeadStage(UUID.randomUUID(), UUID.randomUUID()));
        when(personContactService.findByPersonAndType(existing.getId(), PersonContactType.EMAIL))
                .thenReturn(List.of(email));
        when(leadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setEmail("AN@EXAMPLE.COM");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(existing.getId());
        var response = leadService.createLead(request);

        assertThat(response.getEmail()).isEqualTo("an@example.com");
        verify(personContactService, never()).create(eq(existing), eq(PersonContactType.EMAIL), any(), any());
    }

    @Test
    void existingPersonWithOpenLeadCannotGetAnotherOpenLead() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));
        when(leadRepository.findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(Lead.builder().status(LeadStatus.OPEN).build()));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(existing.getId());
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_ALREADY_OPEN));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void counselorCannotSelectPersonManagedByAnotherCounselor() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("COUNSELOR").build()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));
        when(leadRepository.findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(Lead.builder().assignedTo(UUID.randomUUID()).build()));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(existing.getId());
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_NOT_FOUND));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void creatorCannotReusePersonAfterItsLeadWasAssignedToAnotherCounselor() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("COUNSELOR").build()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An")
                .createdBy(actor).build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));
        when(leadRepository.findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(Lead.builder().assignedTo(UUID.randomUUID()).build()));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(existing.getId());
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_NOT_FOUND));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void oldCounselorCannotSeeThatPersonNowHasAnOpenLeadWithAnotherCounselor() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("COUNSELOR").build()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        Lead oldLead = Lead.builder().status(LeadStatus.LOST).assignedTo(actor.getId()).build();
        Lead currentLead = Lead.builder().status(LeadStatus.OPEN).assignedTo(UUID.randomUUID()).build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));
        when(leadRepository.findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(currentLead, oldLead));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(existing.getId());
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_NOT_FOUND));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void selectedPersonMustReallyMatchNameAndPhone() {
        User actor = User.builder().id(UUID.randomUUID()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(UUID.randomUUID());
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_NOT_FOUND));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void selectedPersonWhoAlreadyConvertedCannotGetNewLead() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        Person existing = Person.builder().id(UUID.randomUUID()).fullName("Nguyễn Văn An").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(personContactService.findByPhone("0912345678"))
                .thenReturn(List.of(PersonContact.builder().person(existing).build()));
        when(leadRepository.findAllByPersonIdAndDeletedAtIsNullOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(Lead.builder().status(LeadStatus.CONVERTED).build()));

        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        request.setExistingPersonId(existing.getId());
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_ALREADY_CUSTOMER));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void confirmationFieldsMustBeConsistent() {
        LeadCreationRequest request = request("Nguyễn Văn An");
        request.setPersonResolution(PersonResolution.USE_EXISTING_PERSON);
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_CONFIRMATION_INVALID));
        verifyNoInteractions(currentActorService, personContactService, personService, leadRepository);

        request.setPersonResolution(PersonResolution.CREATE_NEW_PERSON);
        request.setExistingPersonId(UUID.randomUUID());
        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PERSON_CONFIRMATION_INVALID));
    }

    @Test
    void assignedCounselorCanReadLeadWithPersonContacts() {
        UUID actorId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        User actor = User.builder().id(actorId).role(Role.builder().code("COUNSELOR").build()).build();
        Lead lead = Lead.builder().id(leadId).personId(personId).assignedTo(actorId).build();
        Person person = Person.builder().id(personId).fullName("Nguyễn Văn An").build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(lead));
        when(personService.getById(personId)).thenReturn(person);
        when(personContactService.findByPersonAndType(personId, PersonContactType.PHONE))
                .thenReturn(List.of(PersonContact.builder().value("0912345678").normalizedValue("+84912345678").build()));
        when(personContactService.findByPersonAndType(personId, PersonContactType.EMAIL))
                .thenReturn(List.of(PersonContact.builder().value("an@example.com").build()));

        var response = leadService.getLeadById(leadId);

        assertThat(response.getId()).isEqualTo(leadId);
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(response.getPhone()).isEqualTo("+84912345678");
        assertThat(response.getEmail()).isEqualTo("an@example.com");
        assertThat(response.getPersonId()).isEqualTo(personId);
        assertThat(response.getAssignedTo()).isEqualTo(actorId);
    }

    @Test
    void otherCounselorGetsNotFoundWithoutReadingPerson() {
        UUID leadId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("COUNSELOR").build()).build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(
                Lead.builder().id(leadId).assignedTo(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> leadService.getLeadById(leadId))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_NOT_FOUND));
        verifyNoInteractions(personService, personContactService);
    }

    @Test
    void blankCampaignNameClearsItWithoutChangingOtherFields() {
        UUID leadId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("COUNSELOR").build()).build();
        Lead lead = Lead.builder().id(leadId).personId(personId).assignedTo(actor.getId())
                .sourceId(sourceId).campaignName("Chiến dịch cũ")
                .estimatedBudget(new BigDecimal("50000000.00")).build();
        LeadUpdateRequest request = new LeadUpdateRequest();
        request.setCampaignName("   ");
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.saveAndFlush(lead)).thenReturn(lead);
        when(personService.getById(personId)).thenReturn(Person.builder().id(personId).build());

        var response = leadService.updateLead(leadId, request);

        assertThat(response.getCampaignName()).isNull();
        assertThat(response.getSourceId()).isEqualTo(sourceId);
        assertThat(response.getEstimatedBudget()).isEqualByComparingTo("50000000.00");
        assertThat(lead.getUpdatedBy()).isSameAs(actor);
        verifyNoInteractions(leadCatalogService);
    }

    @Test
    void updatesOnlyRequestedFieldsAfterValidatingSource() {
        UUID leadId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID newSourceId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        Lead lead = Lead.builder().id(leadId).personId(personId)
                .campaignName("Chiến dịch cũ").build();
        LeadUpdateRequest request = new LeadUpdateRequest();
        request.setSourceId(newSourceId);
        request.setEstimatedBudget(new BigDecimal("10000000.00"));
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(lead));
        when(leadCatalogService.resolveActiveSourceId(newSourceId)).thenReturn(newSourceId);
        when(leadRepository.saveAndFlush(lead)).thenReturn(lead);
        when(personService.getById(personId)).thenReturn(Person.builder().id(personId).build());

        var response = leadService.updateLead(leadId, request);

        assertThat(response.getSourceId()).isEqualTo(newSourceId);
        assertThat(response.getEstimatedBudget()).isEqualByComparingTo("10000000.00");
        assertThat(response.getCampaignName()).isEqualTo("Chiến dịch cũ");
        assertThat(response.getAssignedTo()).isNull();
    }

    @Test
    void counselorCannotUpdateAnotherCounselorsLead() {
        UUID leadId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("COUNSELOR").build()).build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(
                Lead.builder().assignedTo(UUID.randomUUID()).build()));
        LeadUpdateRequest request = new LeadUpdateRequest();
        request.setCampaignName("Mới");

        assertThatThrownBy(() -> leadService.updateLead(leadId, request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_NOT_FOUND));
        verify(leadRepository, never()).saveAndFlush(any());
        verifyNoInteractions(personService, leadCatalogService);
    }

    @Test
    void convertedLeadCannotBeUpdated() {
        UUID leadId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(
                Lead.builder().status(LeadStatus.CONVERTED).build()));
        LeadUpdateRequest request = new LeadUpdateRequest();
        request.setCampaignName("Mới");

        assertThatThrownBy(() -> leadService.updateLead(leadId, request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_NOT_MODIFIABLE));
        verify(leadRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidSourceLeavesLeadUnchanged() {
        UUID leadId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        Lead lead = Lead.builder().campaignName("Cũ").build();
        LeadUpdateRequest request = new LeadUpdateRequest();
        request.setSourceId(sourceId);
        request.setCampaignName("Mới");
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(lead));
        when(leadCatalogService.resolveActiveSourceId(sourceId))
                .thenThrow(new AppException(ErrorCode.LEAD_SOURCE_NOT_FOUND));

        assertThatThrownBy(() -> leadService.updateLead(leadId, request))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_SOURCE_NOT_FOUND));
        assertThat(lead.getCampaignName()).isEqualTo("Cũ");
        verify(leadRepository, never()).saveAndFlush(any());
    }

    @Test
    void listsOnePageWithPersonAndContactsLoadedInBatches() {
        UUID personId = UUID.randomUUID();
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        Person person = Person.builder().id(personId).fullName("Nguyễn Văn An").build();
        Lead lead = Lead.builder().id(UUID.randomUUID()).personId(personId).build();
        LeadFilterRequest filter = new LeadFilterRequest();
        filter.setSize(10);
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        when(leadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lead)));
        when(personService.findActiveByIds(any())).thenReturn(List.of(person));
        when(personContactService.findPhonesAndEmailsForPersons(any())).thenReturn(List.of(
                PersonContact.builder().person(person).contactType(PersonContactType.PHONE)
                        .normalizedValue("+84912345678").build(),
                PersonContact.builder().person(person).contactType(PersonContactType.EMAIL)
                        .value("an@example.com").build()));

        var response = leadService.getAllLeads(filter);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(response.getContent().get(0).getPhone()).isEqualTo("+84912345678");
        assertThat(response.getContent().get(0).getEmail()).isEqualTo("an@example.com");
        verify(personService).findActiveByIds(List.of(personId));
        verify(personContactService).findPhonesAndEmailsForPersons(List.of(personId));
        verify(personService, never()).getById(any());
    }

    @Test
    void rejectsUnknownSortFieldsAndDirections() {
        User actor = User.builder().id(UUID.randomUUID())
                .role(Role.builder().code("MANAGER").build()).build();
        when(currentActorService.requireCurrentActor()).thenReturn(actor);
        LeadFilterRequest filter = new LeadFilterRequest();
        filter.setSortBy("personId.hiddenField");
        assertThatThrownBy(() -> leadService.getAllLeads(filter))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));

        filter.setSortBy("createdAt");
        filter.setSortDir("SIDEWAYS");
        assertThatThrownBy(() -> leadService.getAllLeads(filter))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
        verifyNoInteractions(leadRepository);
    }

    private LeadCreationRequest request(String fullName) {
        return LeadCreationRequest.builder().fullName(fullName).phone("0912345678").build();
    }
}
