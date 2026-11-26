package com.crm.korean_pathway_crm.lead.controller;

import com.crm.korean_pathway_crm.lead.dto.request.LeadStageUpdateRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.dto.response.LeadStageResponse;
import com.crm.korean_pathway_crm.lead.service.LeadStageService;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads/{leadId}")
@RequiredArgsConstructor
public class LeadStageController {
    private final LeadStageService leadStageService;

    @GetMapping("/stages")
    public ApiResponse<List<LeadStageResponse>> getAvailableStages(@PathVariable UUID leadId) {
        return ApiResponse.<List<LeadStageResponse>>builder()
                .result(leadStageService.getAvailableStages(leadId)).build();
    }

    @PatchMapping("/stage")
    public ApiResponse<LeadResponse> updateStage(@PathVariable UUID leadId,
                                               @Valid @RequestBody LeadStageUpdateRequest request) {
        return ApiResponse.<LeadResponse>builder()
                .result(leadStageService.updateStage(leadId, request)).build();
    }
}
