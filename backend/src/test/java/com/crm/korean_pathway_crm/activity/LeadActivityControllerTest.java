package com.crm.korean_pathway_crm.activity;

import com.crm.korean_pathway_crm.activity.controller.LeadActivityController;
import com.crm.korean_pathway_crm.activity.enums.ActivityType;
import com.crm.korean_pathway_crm.activity.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadActivityControllerTest {
    @Test
    void createRouteValidatesBodyAndPassesLeadIdToService() throws Exception {
        ActivityService service = mock(ActivityService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LeadActivityController(service)).build();
        UUID leadId = UUID.randomUUID();

        mvc.perform(post("/api/v1/leads/{leadId}/activities", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityType\":\"CALL\",\"summary\":\"Đã tư vấn\"}"))
                .andExpect(status().isCreated());

        verify(service).createForLead(eq(leadId), argThat(request ->
                request.getActivityType() == ActivityType.CALL
                        && "Đã tư vấn".equals(request.getSummary())));
    }

    @Test
    void blankSummaryReturnsBadRequestBeforeService() throws Exception {
        ActivityService service = mock(ActivityService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LeadActivityController(service)).build();

        mvc.perform(post("/api/v1/leads/{leadId}/activities", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityType\":\"CALL\",\"summary\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
