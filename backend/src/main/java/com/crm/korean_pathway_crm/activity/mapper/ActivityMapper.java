package com.crm.korean_pathway_crm.activity.mapper;

import com.crm.korean_pathway_crm.activity.dto.request.ActivityCreationRequest;
import com.crm.korean_pathway_crm.activity.dto.response.ActivityResponse;
import com.crm.korean_pathway_crm.activity.entity.Activity;
import com.crm.korean_pathway_crm.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ActivityMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "leadId", source = "leadId")
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "studentProfileId", ignore = true)
    @Mapping(target = "ownerId", source = "actor.id")
    @Mapping(target = "occurredAt", ignore = true)
    @Mapping(target = "createdBy", source = "actor")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", source = "actor")
    @Mapping(target = "updatedAt", ignore = true)
    Activity toEntity(ActivityCreationRequest request, UUID leadId, User actor);

    @Mapping(target = "createdById", source = "createdBy.id")
    ActivityResponse toResponse(Activity activity);
}
