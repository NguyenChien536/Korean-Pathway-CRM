package com.crm.korean_pathway_crm.lead.controller;

import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.service.LeadService;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {
    private final LeadService leadService;

    @PostMapping
    public ApiResponse<LeadResponse> createLead(@Valid @RequestBody LeadCreationRequest request) {
        return ApiResponse.<LeadResponse>builder()
                .result(leadService.createLead(request))
                .build();
    }
}
