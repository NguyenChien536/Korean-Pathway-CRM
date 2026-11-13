package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeadCreationRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void phoneAndNameAreRequiredButEmailIsOptional() {
        LeadCreationRequest request = LeadCreationRequest.builder()
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .build();

        assertThat(validator.validate(request)).isEmpty();

        request.setPhone("");
        assertThat(validator.validate(request)).anyMatch(error -> error.getPropertyPath().toString().equals("phone"));

        request.setPhone(null);
        assertThat(validator.validate(request)).anyMatch(error -> error.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void invalidOptionalEmailIsRejectedWhenProvided() {
        LeadCreationRequest request = LeadCreationRequest.builder()
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .email("invalid-email")
                .build();

        assertThat(validator.validate(request)).anyMatch(error -> error.getPropertyPath().toString().equals("email"));
    }
}
