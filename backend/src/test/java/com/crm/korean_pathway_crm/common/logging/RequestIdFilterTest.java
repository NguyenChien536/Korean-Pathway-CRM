package com.crm.korean_pathway_crm.common.logging;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesServerRequestIdAndClearsLogContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "client-controlled-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            String requestId = response.getHeader(RequestIdFilter.HEADER_NAME);
            assertThat(UUID.fromString(requestId)).isNotNull();
            assertThat(requestId).isNotEqualTo("client-controlled-value");
            assertThat(MDC.get("requestId")).isEqualTo(requestId);
        });

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void clearsLogContextWhenRequestFails() {
        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> {
                    throw new ServletException("request failed");
                }
        )).isInstanceOf(ServletException.class);

        assertThat(MDC.get("requestId")).isNull();
    }
}
