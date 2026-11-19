package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import com.crm.korean_pathway_crm.lead.entity.Lead;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        var response = leadService.createLead(request("Nguyễn Văn An"));

        assertThat(response.getPersonId()).isEqualTo(person.getId());
        assertThat(response.getAssignedTo()).isEqualTo(actor.getId());
        assertThat(response.getPhone()).isEqualTo("+84912345678");
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

    private LeadCreationRequest request(String fullName) {
        return LeadCreationRequest.builder().fullName(fullName).phone("0912345678").build();
    }
}
