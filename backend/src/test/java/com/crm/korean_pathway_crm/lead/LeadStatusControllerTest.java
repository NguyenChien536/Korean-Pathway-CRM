package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.lead.controller.LeadStatusController;
import com.crm.korean_pathway_crm.lead.service.LeadStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadStatusControllerTest {
    @Test
    void lostRoutePassesLeadIdAndReasonToService() throws Exception {
        LeadStatusService service = mock(LeadStatusService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LeadStatusController(service)).build();
        UUID leadId = UUID.randomUUID();
        mvc.perform(patch("/api/v1/leads/{leadId}/lost", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Chưa đủ tài chính\"}"))
                .andExpect(status().isOk());
        verify(service).markLost(eq(leadId), argThat(request -> "Chưa đủ tài chính".equals(request.getReason())));
    }

    @Test
    void missingBlankOrTooLongReasonIsRejectedBeforeService() throws Exception {
        LeadStatusService service = mock(LeadStatusService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LeadStatusController(service)).build();
        for (String body : new String[]{"{}", "{\"reason\":\"   \"}", "{\"reason\":\"" + "a".repeat(501) + "\"}"}) {
            mvc.perform(patch("/api/v1/leads/{leadId}/lost", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
