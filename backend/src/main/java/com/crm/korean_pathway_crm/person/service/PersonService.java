package com.crm.korean_pathway_crm.person.service;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.person.dto.request.PersonCreationRequest;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.repository.PersonRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public Person getById(UUID personId) {
        return personRepository.findById(personId)
                .filter(person -> person.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
    }

    // When called by LeadService, this save joins its transaction so Person,
    // PersonContact and Lead can be committed or rolled back together.
    @Transactional
    public Person create(PersonCreationRequest request, User actor) {
        if (request == null || !StringUtils.hasText(request.getFullName())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Person person = Person.builder()
                .fullName(request.getFullName().trim().replaceAll("\\s+", " "))
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .nationality(request.getNationality())
                .notes(request.getNotes())
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        return personRepository.save(person);
    }
}
