package com.crm.korean_pathway_crm.person.service;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.entity.PersonContact;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import com.crm.korean_pathway_crm.person.repository.PersonContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonContactService {
    private final PersonContactRepository personContactRepository;

    // The caller must apply the same normalization rule used when saving contacts.
    @Transactional(readOnly = true)
    public List<PersonContact> findByTypeAndNormalizedValue(
            PersonContactType contactType, String normalizedValue) {
        if (contactType == null || !StringUtils.hasText(normalizedValue)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return personContactRepository.findAllByContactTypeAndNormalizedValue(
                contactType, normalizedValue.trim());
    }

    @Transactional
    public PersonContact create(
            Person person,
            PersonContactType contactType,
            String value,
            String normalizedValue
    ) {
        if (person == null || contactType == null
                || !StringUtils.hasText(value) || !StringUtils.hasText(normalizedValue)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        PersonContact contact = PersonContact.builder()
                .person(person)
                .contactType(contactType)
                .value(value.trim())
                .normalizedValue(normalizedValue.trim())
                .build();
        return personContactRepository.save(contact);
    }
}
