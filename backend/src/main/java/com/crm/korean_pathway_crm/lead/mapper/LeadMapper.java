package com.crm.korean_pathway_crm.lead.mapper;

import com.crm.korean_pathway_crm.lead.dto.request.LeadCreationRequest;
import com.crm.korean_pathway_crm.lead.dto.request.LeadUpdateRequest;
import com.crm.korean_pathway_crm.lead.dto.response.LeadResponse;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.service.LeadCatalogService;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.user.entity.User;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface LeadMapper {
    @Mapping(target = "id",ignore = true)
    @Mapping(target = "personId", source = "person.id")        //Phải tạo Person trước mới có ID.
    @Mapping(target = "sourceId", source = "sourceId")
    @Mapping(target = "campaignName", ignore = true)
    @Mapping(target = "pipelineId", source = "initialStage.pipelineId")      //Do hệ thống tự gán stage mặc định ban đầu.
    @Mapping(target = "stageId", source = "initialStage.stageId")         //Do hệ thống tự gán stage mặc định ban đầu.
    @Mapping(target = "assignedTo", source = "actor.id")      //Gán cho Counselor phụ trách
    @Mapping(target = "status", constant = "OPEN")          //mdinh la open
    @Mapping(target = "currencyCode", constant = "VND")    //Mặc định là VND.
    @Mapping(target = "estimatedBudget", ignore = true) //Chưa có lúc mới tạo.
    @Mapping(target = "lostReason", ignore = true)      //Chưa có lúc mới tạo.
    @Mapping(target = "firstContactAt", ignore = true)  //Chưa có lúc mới tạo.
    @Mapping(target = "convertedAt", ignore = true)     //Chưa có lúc mới tạo.
    @Mapping(target = "deletedAt", ignore = true)        //Chưa có lúc mới tạo.
    @Mapping(target = "createdBy", source = "actor")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", source = "actor")
    @Mapping(target = "updatedAt", ignore = true)
    Lead toLead(
            LeadCreationRequest request,
            Person person,
            LeadCatalogService.InitialLeadStage initialStage,
            UUID sourceId,
            User actor
            );

    // 1. Hàm chính: Đầy đủ các nguồn dữ liệu (dùng cho createLead)
    @Mapping( target = "id", source = "lead.id")
    @Mapping(target = "fullName", source = "person.fullName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "sharedPhoneWarning", source = "hasSharedPhone")
    @Mapping(target = "createdAt", source = "lead.createdAt")
    @Mapping(target = "updatedAt", source = "lead.updatedAt")
    // các nguồn còn lại tự động map vì trùng tên rồi
    LeadResponse toLeadResponse(
            Lead lead,
            Person person,
            String phone,
            String email,
            boolean hasSharedPhone
    );
    // 2. Hàm Overload tiện ích: Dùng cho getLeadById (mặc định sharedPhoneWarning = false)
    default LeadResponse toLeadResponse(Lead lead, Person person, String phone, String email) {
        return toLeadResponse(lead, person, phone, email, false);
    }

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            builder = @org.mapstruct.Builder(disableBuilder = true)
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "sourceId", ignore = true)
    @Mapping(target = "pipelineId", ignore = true)
    @Mapping(target = "stageId", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "campaignName", ignore = true)
    @Mapping(target = "currencyCode", ignore = true)
    @Mapping(target = "lostReason", ignore = true)
    @Mapping(target = "firstContactAt", ignore = true)
    @Mapping(target = "convertedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateLead (@MappingTarget Lead lead, LeadUpdateRequest request);
}
