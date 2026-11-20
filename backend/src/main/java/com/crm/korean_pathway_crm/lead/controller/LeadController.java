package com.crm.korean_pathway_crm.lead.controller;

import com.crm.korean_pathway_crm.common.dto.PageResponse;
import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadFilterRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadUpdateRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.service.LeadService;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @GetMapping("/{leadId}")
    public ApiResponse<LeadResponse> getLeadById(@PathVariable UUID leadId) {
        return ApiResponse.<LeadResponse>builder()
                .result(leadService.getLeadById(leadId))
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<LeadResponse>> getAllLeads(@Valid @ModelAttribute LeadFilterRequest request) {
        return ApiResponse.<PageResponse<LeadResponse>>builder()
                .result(leadService.getAllLeads(request))
                .build();
    }

    @PatchMapping("/{leadId}")
    public ApiResponse<LeadResponse> updateLead(@PathVariable UUID leadId, @Valid @RequestBody LeadUpdateRequest request) {
        return ApiResponse.<LeadResponse>builder()
                .result(leadService.updateLead(leadId, request))
                .build();
    }
}
