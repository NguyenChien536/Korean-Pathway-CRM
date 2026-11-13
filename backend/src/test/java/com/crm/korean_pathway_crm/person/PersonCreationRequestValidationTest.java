package com.crm.korean_pathway_crm.person;

import com.crm.korean_pathway_crm.person.dto.request.PersonCreationRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PersonCreationRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void initialLeadPersonNeedsOnlyName() {
        PersonCreationRequest request = PersonCreationRequest.builder()
                .fullName("Nguyen Van A")
                .build();

        assertThat(validator.validate(request)).isEmpty();

        request.setDateOfBirth(LocalDate.of(2005, 5, 12));
        assertThat(validator.validate(request)).isEmpty();

        request.setFullName(" ");
        assertThat(validator.validate(request))
                .anyMatch(error -> error.getPropertyPath().toString().equals("fullName"));
    }
}
