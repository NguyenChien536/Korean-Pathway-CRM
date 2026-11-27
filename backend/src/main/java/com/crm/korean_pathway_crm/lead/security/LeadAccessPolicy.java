package com.crm.korean_pathway_crm.lead.security;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class LeadAccessPolicy {
    // Actor đã được CurrentActorService xác thực. Policy chỉ quyết định phạm vi bản ghi,
    // còn quyền gọi chức năng vẫn do @PreAuthorize kiểm tra tại từng service.
    public boolean canAccessAll(User actor) {
        if (actor == null || actor.getRole() == null) {
            return false;
        }
        String roleCode = actor.getRole().getCode();
        return SystemRoleCode.SYSTEM_ADMIN.name().equals(roleCode)
                || SystemRoleCode.MANAGER.name().equals(roleCode);
    }

    public void ensureAccessible(Lead lead, User actor) {
        if (actor == null || actor.getId() == null) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (lead == null || (!canAccessAll(actor) && !actor.getId().equals(lead.getAssignedTo()))) {
            // Không tiết lộ Lead của counselor khác hoặc Lead chưa được giao.
            throw new AppException(ErrorCode.LEAD_NOT_FOUND);
        }
    }
}
