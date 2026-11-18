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
import com.crm.korean_pathway_crm.person.service.PersonContactService;
import com.crm.korean_pathway_crm.person.service.PersonService;
import com.crm.korean_pathway_crm.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    private LeadCreationRequest request(String fullName) {
        return LeadCreationRequest.builder().fullName(fullName).phone("0912345678").build();
    }
}
