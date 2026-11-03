package com.crm.korean_pathway_crm.user.dto.request;

import com.crm.korean_pathway_crm.common.dto.BaseFilterRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFilterRequest extends BaseFilterRequest {
    Boolean status;
    UUID roleId;
}
