package com.crm.korean_pathway_crm.exception;

import com.crm.korean_pathway_crm.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ErrorProbe())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void errorCodesAreUniqueAndAlwaysRepresentHttpErrors() {
        assertThat(Arrays.stream(ErrorCode.values()).map(ErrorCode::getCode).toList()).doesNotHaveDuplicates();
        for (ErrorCode error : ErrorCode.values()) {
            assertThat(error.getMessage()).isNotBlank();
            assertThat(error.getHttpStatus().isError()).isTrue();
        }
    }

    @ParameterizedTest
    @CsvSource({"conflict,409,9005", "lock,409,9006", "database,503,9008", "upload,413,9007",
            "forbidden,403,1006", "authentication,401,1011", "not-found,404,9000", "unexpected,500,9999"})
    void convertsFailuresIntoSafeApiResponses(String failure, int statusCode, int code) throws Exception {
        mvc.perform(get("/probe/failure").param("kind", failure))
                .andExpect(status().is(statusCode)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(content().string(not(containsString("internal-sensitive-detail"))));
    }

    @Test
    void missingAndMalformedParametersHaveDifferentCodes() throws Exception {
        mvc.perform(get("/probe/parameter")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9004));
        mvc.perform(get("/probe/parameter").param("id", "not-a-uuid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(9003));
    }

    @Test
    void preservesHttpStatusAndAllowHeaderForUnsupportedMethod() throws Exception {
        mvc.perform(put("/probe/body")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.code").value(9001));
    }

    @Test
    void handlesInvalidJsonValidationAndUnsupportedContentType() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1010));
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1010));
        mvc.perform(post("/probe/body").contentType(MediaType.TEXT_PLAIN).content("plain-text"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value(9002));
    }

    @RestController
    static class ErrorProbe {
        record Body(@NotBlank String name) {}

        @PostMapping("/probe/body")
        Map<String, String> body(@Valid @RequestBody Body body) {
            return Map.of("name", body.name());
        }

        @GetMapping("/probe/parameter")
        String parameter(@RequestParam UUID id) {
            return id.toString();
        }

        @GetMapping("/probe/failure")
        String failure(@RequestParam String kind) {
            throw switch (kind) {
                case "conflict" -> new DataIntegrityViolationException("internal-sensitive-detail");
                case "lock" -> new OptimisticLockingFailureException("internal-sensitive-detail");
                case "database" -> new DataAccessResourceFailureException("internal-sensitive-detail");
                case "upload" -> new MaxUploadSizeExceededException(1024);
                case "forbidden" -> new AccessDeniedException("internal-sensitive-detail");
                case "authentication" -> new BadCredentialsException("internal-sensitive-detail");
                case "not-found" -> new ResourceNotFoundException("internal-sensitive-detail");
                default -> new IllegalStateException("internal-sensitive-detail");
            };
        }
    }
}
