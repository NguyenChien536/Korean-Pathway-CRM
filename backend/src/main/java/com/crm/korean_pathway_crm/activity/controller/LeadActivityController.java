package com.crm.korean_pathway_crm.activity.controller;

import com.crm.korean_pathway_crm.activity.dto.request.ActivityCreationRequest;
import com.crm.korean_pathway_crm.activity.dto.response.ActivityResponse;
import com.crm.korean_pathway_crm.activity.service.ActivityService;
import com.crm.korean_pathway_crm.common.dto.PageResponse;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads/{leadId}/activities")
@RequiredArgsConstructor
public class LeadActivityController {
    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<ApiResponse<ActivityResponse>> createForLead(
            @PathVariable UUID leadId, @Valid @RequestBody ActivityCreationRequest request) {
        ActivityResponse result = activityService.createForLead(leadId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ActivityResponse>builder().result(result).build());
    }

    @GetMapping
    public ApiResponse<PageResponse<ActivityResponse>> getForLead(
            @PathVariable UUID leadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ActivityResponse>>builder()
                .result(activityService.getForLead(leadId, page, size))
                .build();
    }
}
