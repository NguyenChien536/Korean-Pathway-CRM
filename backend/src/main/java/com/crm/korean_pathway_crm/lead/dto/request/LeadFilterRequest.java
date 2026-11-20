package com.crm.korean_pathway_crm.lead.dto.request;

import com.crm.korean_pathway_crm.common.dto.BaseFilterRequest;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeadFilterRequest extends BaseFilterRequest {
    LeadStatus status;      // OPEN, CONVERTED, LOST, ARCHIVED
    UUID pipelineId;        // Lọc theo đường ống
    UUID stageId;           // Lọc theo cột Kanban cụ thể
    UUID sourceId;          // Lọc theo nguồn (Facebook, Zalo...)
    UUID assignedTo;        // Lọc theo Counselor
}
