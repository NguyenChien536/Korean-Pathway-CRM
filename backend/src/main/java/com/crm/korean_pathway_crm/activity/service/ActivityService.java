package com.crm.korean_pathway_crm.activity.service;

import com.crm.korean_pathway_crm.activity.dto.request.ActivityCreationRequest;
import com.crm.korean_pathway_crm.activity.dto.response.ActivityResponse;
import com.crm.korean_pathway_crm.activity.entity.Activity;
import com.crm.korean_pathway_crm.activity.mapper.ActivityMapper;
import com.crm.korean_pathway_crm.activity.repository.ActivityRepository;
import com.crm.korean_pathway_crm.common.dto.PageResponse;
import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final LeadRepository leadRepository;
    private final CurrentActorService currentActorService;
    private final ActivityMapper activityMapper;

    @Transactional
    @PreAuthorize("hasAuthority('ACTIVITIES.CREATE')")
    public ActivityResponse createForLead(UUID leadId, ActivityCreationRequest request) {
        if (request == null || request.getActivityType() == null
                || request.getSummary() == null || request.getSummary().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User actor = currentActorService.requireCurrentActor();
        Lead lead = requireAccessibleLead(leadId, actor);
        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new AppException(ErrorCode.LEAD_ACTIVITY_NOT_ALLOWED);
        }

        // ID đích và người ghi do backend quyết định; client chỉ cung cấp nội dung tương tác.
        Activity activity = activityMapper.toEntity(request, lead.getId(), actor);
        activity.setSummary(request.getSummary().strip());
        return activityMapper.toResponse(activityRepository.saveAndFlush(activity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACTIVITIES.READ')")
    public PageResponse<ActivityResponse> getForLead(UUID leadId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }

        User actor = currentActorService.requireCurrentActor();
        requireAccessibleLead(leadId, actor);
        // Điều kiện leadId nằm trong truy vấn DB trước khi phân trang và đếm tổng.
        Page<Activity> activities = activityRepository.findByLeadId(leadId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
        return PageResponse.of(activities.map(activityMapper::toResponse));
    }

    private Lead requireAccessibleLead(UUID leadId, User actor) {
        if (leadId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Lead lead = leadRepository.findByIdAndDeletedAtIsNull(leadId)
                .orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND));

        String roleCode = actor.getRole().getCode();
        boolean canAccessAll = SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);
        if (!canAccessAll && !actor.getId().equals(lead.getAssignedTo())) {
            // Cùng một 404 cho ID không tồn tại và Lead ngoài phạm vi, tránh lộ dữ liệu của người khác.
            throw new AppException(ErrorCode.LEAD_NOT_FOUND);
        }
        return lead;
    }
}
