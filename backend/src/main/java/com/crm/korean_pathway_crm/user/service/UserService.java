package com.crm.korean_pathway_crm.user.service;

import com.crm.korean_pathway_crm.auth.service.RefreshTokenService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.user.dto.UserCreationRequest;
import com.crm.korean_pathway_crm.user.dto.UserResponse;
import com.crm.korean_pathway_crm.user.dto.UserUpdateRequest;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.mapper.UserMapper;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String USERNAME_UNIQUE_INDEX = "uq_users_username_lower";
    private static final String EMAIL_UNIQUE_INDEX = "uq_users_email_lower";
    private static final String PHONE_UNIQUE_INDEX = "uq_users_phone";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        User actor = requirePermission("USERS.MANAGE");
        String username = normalizeRequired(request.getUsername());
        String email = normalizeRequired(request.getEmail());
        String phone = normalizeOptional(request.getPhone());

        ensureUsernameAvailable(username, null);
        ensureEmailAvailable(email, null);
        ensurePhoneAvailable(phone, null);
        ensurePasswordConfirmation(request.getPassword(), request.getConfirmPassword());

        Role role = findAssignableRole(request.getRoleId(), actor);
        User user = userMapper.toUser(request);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(normalizeRequired(request.getFullName()));
        user.setPhone(phone);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setPasswordChangedAt(now);
        user.setCreatedBy(actor);
        user.setUpdatedBy(actor);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userMapper.toUserResponse(saveUser(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        requirePermission("USERS.READ");
        return userMapper.toUserResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        requirePermission("USERS.READ");
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"))
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User actor = requirePermission("USERS.MANAGE");
        User user = findUser(id);

        if (request.getUsername() != null) {
            ensureUsernameAvailable(normalizeRequired(request.getUsername()), id);
        }
        if (request.getEmail() != null) {
            ensureEmailAvailable(normalizeRequired(request.getEmail()), id);
        }
        String requestedPhone = request.getPhone() == null ? null : normalizeOptional(request.getPhone());
        if (requestedPhone != null) {
            ensurePhoneAvailable(requestedPhone, id);
        }

        boolean roleChanged = false;
        if (request.getRoleId() != null && !request.getRoleId().equals(user.getRole().getId())) {
            Role role = findAssignableRole(request.getRoleId(), actor);
            user.setRole(role);
            roleChanged = true;
        }

        userMapper.updateUser(user, request);
        if (request.getUsername() != null) {
            user.setUsername(normalizeRequired(request.getUsername()));
        }
        if (request.getEmail() != null) {
            user.setEmail(normalizeRequired(request.getEmail()));
        }
        if (request.getFullName() != null) {
            user.setFullName(normalizeRequired(request.getFullName()));
        }
        if (request.getPhone() != null) {
            user.setPhone(requestedPhone);
        }

        boolean passwordChanged = updatePasswordIfRequested(user, request);
        user.setUpdatedBy(actor);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        User savedUser = saveUser(user);
        if (roleChanged || passwordChanged) {
            refreshTokenService.revokeAllUserRefreshTokens(savedUser.getUsername());
        }
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User actor = requirePermission("USERS.MANAGE");
        User user = findUser(userId);

        // Keep audit and business-history references intact; disable the account instead of deleting it.
        if (actor.getId().equals(user.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (!user.isActive()) {
            return;
        }

        user.setActive(false);
        user.setUpdatedBy(actor);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        refreshTokenService.revokeAllUserRefreshTokens(user.getUsername());
    }

    private boolean updatePasswordIfRequested(User user, UserUpdateRequest request) {
        String password = request.getPassword();
        String confirmation = request.getConfirmPassword();
        if (!StringUtils.hasText(password)) {
            if (StringUtils.hasText(confirmation)) {
                throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
            }
            return false;
        }

        ensurePasswordConfirmation(password, confirmation);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        return true;
    }

    private User requirePermission(String permissionCode) {
        User actor = currentActor();
        if (!roleRepository.hasPermission(actor.getRole().getId(), permissionCode)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return actor;
    }

    private User currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }

        UUID actorId;
        try {
            actorId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }
        User actor = findUser(actorId);
        if (!actor.isActive() || actor.getRole() == null || actor.getRole().getStatus() != RoleStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return actor;
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() != null && SystemRoleCode.SYSTEM_ADMIN.name().equals(user.getRole().getCode());
    }

    private Role findAssignableRole(UUID roleId, User actor) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (role.getStatus() != RoleStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROLE_INACTIVE);
        }
        if (SystemRoleCode.SYSTEM_ADMIN.name().equals(role.getCode()) && !isSystemAdmin(actor)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return role;
    }

    private void ensurePasswordConfirmation(String password, String confirmation) {
        if (!Objects.equals(password, confirmation)) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
    }

    private void ensureUsernameAvailable(String username, UUID excludedUserId) {
        boolean exists = excludedUserId == null
                ? userRepository.existsByUsernameIgnoreCase(username)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, excludedUserId);
        if (exists) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }

    private void ensureEmailAvailable(String email, UUID excludedUserId) {
        boolean exists = excludedUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludedUserId);
        if (exists) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void ensurePhoneAvailable(String phone, UUID excludedUserId) {
        if (phone == null) {
            return;
        }
        boolean exists = excludedUserId == null
                ? userRepository.existsByPhone(phone)
                : userRepository.existsByPhoneAndIdNot(phone, excludedUserId);
        if (exists) {
            throw new AppException(ErrorCode.USER_PHONE_EXISTED);
        }
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User saveUser(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            String constraintName = findConstraintName(exception);
            if (USERNAME_UNIQUE_INDEX.equals(constraintName)) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            if (EMAIL_UNIQUE_INDEX.equals(constraintName)) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            if (PHONE_UNIQUE_INDEX.equals(constraintName)) {
                throw new AppException(ErrorCode.USER_PHONE_EXISTED);
            }
            throw exception;
        }
    }

    private String findConstraintName(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
