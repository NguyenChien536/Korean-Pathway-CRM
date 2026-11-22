package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.lead.controller.LeadController;
import com.crm.korean_pathway_crm.lead.service.LeadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadControllerTest {
    @Test
    void assignmentRoutePassesLeadAndAssigneeIdsToService() throws Exception {
        LeadService service = mock(LeadService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LeadController(service)).build();
        UUID leadId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        mvc.perform(patch("/api/v1/leads/{leadId}/assignee", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + assigneeId + "\"}"))
                .andExpect(status().isOk());

        verify(service).assignLead(eq(leadId), argThat(request -> assigneeId.equals(request.getAssigneeId())));
    }
}
