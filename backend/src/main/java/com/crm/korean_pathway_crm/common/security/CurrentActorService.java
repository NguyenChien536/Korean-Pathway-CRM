package com.crm.korean_pathway_crm.common.security;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentActorService {
    private final UserRepository userRepository;

    // Resolves who is calling; each business service still checks its own authority and record scope.
    @Transactional(readOnly = true)
    public User requireCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }

        String actorName = authentication.getName();
        if (actorName == null) {
            throw new AppException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }

        UUID actorId;
        try {
            actorId = UUID.fromString(actorName);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }

        User actor = userRepository.findWithRoleById(actorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!actor.isActive() || actor.getRole() == null || actor.getRole().getStatus() != RoleStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return actor;
    }
}
