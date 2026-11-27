package com.crm.korean_pathway_crm.lead.mapper;

import com.crm.korean_pathway_crm.lead.dto.response.LeadStatusResponse;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.entity.LeadStatusHistory;
import com.crm.korean_pathway_crm.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeadStatusMapper {

    LeadStatusResponse toResponse(LeadStatusHistory status);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "leadId", source = "lead.id")
    @Mapping(target = "fromStatus", source = "lead.status")
    @Mapping(target = "toStatus", constant = "LOST")
    @Mapping(target = "changedBy", source = "actor.id")
    @Mapping(target = "changedAt", ignore = true)
    @Mapping(target = "reason", source = "reason")
    LeadStatusHistory toLostHistory(
            Lead lead,
            User actor,
            String reason);


}
