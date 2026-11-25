package com.crm.korean_pathway_crm.activity;

import com.crm.korean_pathway_crm.activity.dto.request.ActivityCreationRequest;
import com.crm.korean_pathway_crm.activity.entity.Activity;
import com.crm.korean_pathway_crm.activity.enums.ActivityType;
import com.crm.korean_pathway_crm.activity.mapper.ActivityMapper;
import com.crm.korean_pathway_crm.activity.repository.ActivityRepository;
import com.crm.korean_pathway_crm.activity.service.ActivityService;
import com.crm.korean_pathway_crm.common.security.CurrentActorService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {
    @Mock ActivityRepository activities;
    @Mock LeadRepository leads;
    @Mock CurrentActorService currentActorService;
    @Spy ActivityMapper mapper = Mappers.getMapper(ActivityMapper.class);
    @InjectMocks ActivityService service;

    @Test
    void assignedCounselorRecordsCallWithoutChoosingOwnerOrTarget() {
        UUID leadId = UUID.randomUUID();
        User counselor = actor("COUNSELOR");
        when(currentActorService.requireCurrentActor()).thenReturn(counselor);
        when(leads.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(
                Lead.builder().id(leadId).assignedTo(counselor.getId()).status(LeadStatus.OPEN).build()));
        when(activities.saveAndFlush(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setId(UUID.randomUUID());
            return activity;
        });
        ActivityCreationRequest request = request();
        request.setSummary("  Đã gọi cho phụ huynh  ");

        var response = service.createForLead(leadId, request);

        ArgumentCaptor<Activity> saved = ArgumentCaptor.forClass(Activity.class);
        verify(activities).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getLeadId()).isEqualTo(leadId);
        assertThat(saved.getValue().getCustomerId()).isNull();
        assertThat(saved.getValue().getStudentProfileId()).isNull();
        assertThat(saved.getValue().getOwnerId()).isEqualTo(counselor.getId());
        assertThat(saved.getValue().getCreatedBy()).isSameAs(counselor);
        assertThat(response.getSummary()).isEqualTo("Đã gọi cho phụ huynh");
        assertThat(response.getCreatedById()).isEqualTo(counselor.getId());
    }

    @Test
    void counselorCannotWriteOrReadAnotherCounselorsLead() {
        UUID leadId = UUID.randomUUID();
        User counselor = actor("COUNSELOR");
        when(currentActorService.requireCurrentActor()).thenReturn(counselor);
        when(leads.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(
                Lead.builder().id(leadId).assignedTo(UUID.randomUUID()).status(LeadStatus.OPEN).build()));

        assertThatThrownBy(() -> service.createForLead(leadId, request()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_NOT_FOUND));
        assertThatThrownBy(() -> service.getForLead(leadId, 0, 20))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_NOT_FOUND));
        verifyNoInteractions(activities);
    }

    @Test
    void closedOrSoftDeletedLeadCannotReceiveActivity() {
        UUID leadId = UUID.randomUUID();
        when(currentActorService.requireCurrentActor()).thenReturn(actor("MANAGER"));
        when(leads.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(
                Lead.builder().id(leadId).status(LeadStatus.CONVERTED).build()), Optional.empty());

        assertThatThrownBy(() -> service.createForLead(leadId, request()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_ACTIVITY_NOT_ALLOWED));
        assertThatThrownBy(() -> service.createForLead(leadId, request()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_NOT_FOUND));
        verifyNoInteractions(activities);
    }

    @Test
    void managerReadsActivitiesWithLeadFilterBeforePagination() {
        UUID leadId = UUID.randomUUID();
        when(currentActorService.requireCurrentActor()).thenReturn(actor("MANAGER"));
        when(leads.findByIdAndDeletedAtIsNull(leadId)).thenReturn(Optional.of(Lead.builder().id(leadId).build()));
        when(activities.findByLeadId(eq(leadId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                Activity.builder().id(UUID.randomUUID()).leadId(leadId).activityType(ActivityType.CALL).build())));

        var response = service.getForLead(leadId, 0, 20);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getLeadId()).isEqualTo(leadId);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(activities).findByLeadId(eq(leadId), page.capture());
        assertThat(page.getValue().getSort().getOrderFor("occurredAt").isDescending()).isTrue();
    }

    @Test
    void invalidPageSizeStopsBeforeDatabaseLookup() {
        assertThatThrownBy(() -> service.getForLead(UUID.randomUUID(), 0, 101))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
        verifyNoInteractions(leads, activities, currentActorService);
    }

    private User actor(String roleCode) {
        return User.builder().id(UUID.randomUUID()).role(Role.builder().code(roleCode).build()).build();
    }

    private ActivityCreationRequest request() {
        ActivityCreationRequest request = new ActivityCreationRequest();
        request.setActivityType(ActivityType.CALL);
        request.setSummary("Đã gọi cho phụ huynh");
        return request;
    }
}
