package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.lead.controller.LeadStageController;
import com.crm.korean_pathway_crm.lead.service.LeadStageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadStageControllerTest {
    @Test
    void routesStageSelectionAndUpdateToService() throws Exception {
        LeadStageService service = mock(LeadStageService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LeadStageController(service)).build();
        UUID leadId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        mvc.perform(get("/api/v1/leads/{leadId}/stages", leadId)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/leads/{leadId}/stage", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stageId + "\"}"))
                .andExpect(status().isOk());
        verify(service).getAvailableStages(leadId);
        verify(service).updateStage(eq(leadId), argThat(request -> stageId.equals(request.getStageId())));
    }

    @Test
    void missingStageIdReturnsBadRequestBeforeService() throws Exception {
        LeadStageService service = mock(LeadStageService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LeadStageController(service)).build();
        mvc.perform(patch("/api/v1/leads/{leadId}/stage", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
