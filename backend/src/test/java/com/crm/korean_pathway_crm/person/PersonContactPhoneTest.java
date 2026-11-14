package com.crm.korean_pathway_crm.person;

import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.entity.PersonContact;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import com.crm.korean_pathway_crm.person.repository.PersonContactRepository;
import com.crm.korean_pathway_crm.person.service.PersonContactService;
import com.crm.korean_pathway_crm.person.service.PhoneNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonContactPhoneTest {
    private final PersonContactRepository repository = mock(PersonContactRepository.class);
    private final PersonContactService service = new PersonContactService(repository, new PhoneNormalizer());

    @Test
    void searchAndSaveUseTheSameCanonicalPhone() {
        Person person = Person.builder().fullName("Nguyen Van A").build();
        when(repository.save(any(PersonContact.class))).thenAnswer(call -> call.getArgument(0));

        PersonContact saved = service.createPhone(person, "0912345678");
        assertThat(saved.getValue()).isEqualTo("0912345678");
        assertThat(saved.getNormalizedValue()).isEqualTo("+84912345678");

        when(repository.findAllByContactTypeAndNormalizedValue(
                PersonContactType.PHONE, "+84912345678")).thenReturn(List.of(saved));
        assertThat(service.findByPhone("+84912345678")).containsExactly(saved);
        verify(repository).findAllByContactTypeAndNormalizedValue(
                PersonContactType.PHONE, saved.getNormalizedValue());
    }
}
