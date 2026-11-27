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

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonContactService {
    private final PersonContactRepository personContactRepository;
    private final PhoneNormalizer phoneNormalizer;

    @Transactional(readOnly = true)
    public List<PersonContact> findByPhone(String phone) {
        String normalizedPhone = phoneNormalizer.normalize(phone);
        return personContactRepository.findAllByContactTypeAndNormalizedValue(
                PersonContactType.PHONE, normalizedPhone);
    }

    @Transactional(readOnly = true)
    public List<PersonContact> findByPersonAndType(UUID personId, PersonContactType type) {
        return personContactRepository.findAllByPersonIdAndContactTypeOrderByPrimaryDescCreatedAtAscIdAsc(personId, type);
    }

    @Transactional(readOnly = true)
    public String findFirstValue(UUID personId, PersonContactType type) {
        // Repository ưu tiên contact chính, rồi đến thời gian tạo và ID để thứ tự ổn định.
        // Dùng chung cho các response; service gọi hàm này phải kiểm tra phạm vi dữ liệu trước.
        return findByPersonAndType(personId, type).stream().findFirst()
                .map(contact -> type == PersonContactType.PHONE
                        ? contact.getNormalizedValue() : contact.getValue())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PersonContact> findPhonesAndEmailsForPersons(Collection<UUID> personIds) {
        return personContactRepository
                .findAllByPersonIdInAndContactTypeInOrderByPersonIdAscContactTypeAscPrimaryDescCreatedAtAscIdAsc(
                        personIds, List.of(PersonContactType.PHONE, PersonContactType.EMAIL));
    }

    // PHONE accepts raw input and applies the shared normalizer before lookup.
    @Transactional(readOnly = true)
    public List<PersonContact> findByTypeAndNormalizedValue(
            PersonContactType contactType, String normalizedValue) {
        if (contactType == null || !StringUtils.hasText(normalizedValue)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        String lookupValue = contactType == PersonContactType.PHONE
                ? phoneNormalizer.normalize(normalizedValue)
                : normalizedValue.trim();
        return personContactRepository.findAllByContactTypeAndNormalizedValue(contactType, lookupValue);
    }

    @Transactional
    public PersonContact createPhone(Person person, String phone) {
        phoneNormalizer.normalize(phone);
        return create(person, PersonContactType.PHONE, phone, null);
    }

    @Transactional
    public PersonContact create(
            Person person,
            PersonContactType contactType,
            String value,
            String normalizedValue
    ) {
        if (person == null || contactType == null || !StringUtils.hasText(value)
                || (contactType != PersonContactType.PHONE && !StringUtils.hasText(normalizedValue))) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String canonicalValue;
        if (contactType == PersonContactType.PHONE) {
            canonicalValue = phoneNormalizer.normalize(value);
            if (normalizedValue != null
                    && !canonicalValue.equals(phoneNormalizer.normalize(normalizedValue))) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            canonicalValue = normalizedValue.trim();
        }

        PersonContact contact = PersonContact.builder()
                .person(person)
                .contactType(contactType)
                .value(value.trim())
                .normalizedValue(canonicalValue)
                .build();
        return personContactRepository.save(contact);
    }
}
