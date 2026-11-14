package com.crm.korean_pathway_crm.person;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.person.service.PhoneNormalizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PhoneNormalizerTest {
    private final PhoneNormalizer normalizer = new PhoneNormalizer();

    @Test
    void vietnameseLocalAndInternationalFormsHaveTheSameKey() {
        assertThat(normalizer.normalize("0912345678")).isEqualTo("+84912345678");
        assertThat(normalizer.normalize("+84912345678")).isEqualTo("+84912345678");
        assertThat(normalizer.normalize("+821012345678")).isEqualTo("+821012345678");
    }

    @Test
    void ambiguousOrMalformedNumbersAreRejected() {
        for (String input : new String[]{"84912345678", "+840912345678", "0912 345 678", "00123456789"}) {
            assertThatExceptionOfType(AppException.class)
                    .isThrownBy(() -> normalizer.normalize(input))
                    .satisfies(error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_PHONE));
        }
    }
}
