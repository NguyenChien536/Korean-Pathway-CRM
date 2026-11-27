package com.crm.korean_pathway_crm.lead.controller;

import com.crm.korean_pathway_crm.lead.dto.request.LeadStatusUpdateRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.service.LeadStatusService;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadStatusController {
    private final LeadStatusService leadStatusService;

    @PatchMapping("/{leadId}/lost")
    public ApiResponse<LeadResponse> markLost(@PathVariable UUID leadId, @Valid @RequestBody LeadStatusUpdateRequest request) {
        return ApiResponse.<LeadResponse>builder()
                .result(leadStatusService.markLost(leadId, request))
                .build();
    }

}
