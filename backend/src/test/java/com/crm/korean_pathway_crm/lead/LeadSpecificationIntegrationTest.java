package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.lead.dto.request.LeadFilterRequest;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.lead.specification.LeadSpecification;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.entity.PersonContact;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import com.crm.korean_pathway_crm.person.repository.PersonContactRepository;
import com.crm.korean_pathway_crm.person.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeadSpecificationIntegrationTest {
    @Autowired LeadRepository leads;
    @Autowired PersonRepository persons;
    @Autowired PersonContactRepository contacts;

    @Test
    void scopesBeforeCountingAndPaginatesSearchResults() {
        UUID counselorA = UUID.randomUUID();
        UUID counselorB = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        Lead first = lead("Nguyễn Văn An", counselorA, pipelineId, stageId);
        lead("Nguyễn Thị Hoa", counselorA, pipelineId, stageId);
        lead("Nguyễn Văn Bình", counselorB, pipelineId, stageId);

        LeadFilterRequest filter = new LeadFilterRequest();
        filter.setSearch("nguyễn");
        filter.setPipelineId(pipelineId);
        Page<Lead> counselorPage = leads.findAll(
                LeadSpecification.of(filter, counselorA, false), PageRequest.of(0, 1));

        assertThat(counselorPage.getTotalElements()).isEqualTo(2);
        assertThat(counselorPage.getTotalPages()).isEqualTo(2);
        assertThat(counselorPage.getContent()).hasSize(1);
        assertThat(counselorPage.getContent().get(0).getAssignedTo()).isEqualTo(counselorA);

        filter.setAssignedTo(counselorB);
        assertThat(leads.findAll(LeadSpecification.of(filter, counselorA, false), PageRequest.of(0, 10)))
                .isEmpty();
        assertThat(leads.findAll(LeadSpecification.of(filter, counselorA, true), PageRequest.of(0, 10))
                .getContent()).extracting(Lead::getAssignedTo).containsExactly(counselorB);

        filter.setAssignedTo(null);
        filter.setPipelineId(UUID.randomUUID());
        assertThat(leads.findAll(LeadSpecification.of(filter, counselorA, false), PageRequest.of(0, 10)))
                .isEmpty();
        assertThat(first.getId()).isNotNull();
    }

    @Test
    void searchesNormalizedPhoneAndExcludesSoftDeletedLeads() {
        UUID counselor = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        Lead active = lead("Trần Văn A", counselor, pipelineId, stageId);
        Person person = persons.findById(active.getPersonId()).orElseThrow();
        contacts.save(PersonContact.builder().person(person).contactType(PersonContactType.PHONE)
                .value("+84912345678").normalizedValue("+84912345678").build());
        Lead deleted = lead("Trần Văn B", counselor, pipelineId, stageId);
        deleted.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        leads.save(deleted);
        Lead hiddenPersonLead = lead("Trần Văn C", counselor, pipelineId, stageId);
        Person hiddenPerson = persons.findById(hiddenPersonLead.getPersonId()).orElseThrow();
        hiddenPerson.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        persons.save(hiddenPerson);

        LeadFilterRequest filter = new LeadFilterRequest();
        filter.setSearch("0912");
        assertThat(leads.findAll(LeadSpecification.of(filter, counselor, false), PageRequest.of(0, 10))
                .getContent()).extracting(Lead::getId).containsExactly(active.getId());

        filter.setSearch("trần");
        assertThat(leads.findAll(LeadSpecification.of(filter, counselor, false), PageRequest.of(0, 10))
                .getContent()).extracting(Lead::getId).containsExactly(active.getId());

        filter.setSearch("%");
        assertThat(leads.findAll(LeadSpecification.of(filter, counselor, false), PageRequest.of(0, 10)))
                .isEmpty();
    }

    private Lead lead(String fullName, UUID assignedTo, UUID pipelineId, UUID stageId) {
        Person person = persons.save(Person.builder().fullName(fullName).build());
        return leads.save(Lead.builder().personId(person.getId()).pipelineId(pipelineId)
                .stageId(stageId).assignedTo(assignedTo).build());
    }
}
