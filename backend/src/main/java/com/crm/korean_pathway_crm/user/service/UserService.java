package com.crm.korean_pathway_crm.user.service;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String USERNAME_UNIQUE_INDEX = "uq_users_username_lower";
    private static final String EMAIL_UNIQUE_INDEX = "uq_users_email_lower";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        String username = normalizeRequired(request.getUsername());
        String email = normalizeRequired(request.getEmail());

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        Role role = findAssignableRole(request.getRoleId());
        User user = userMapper.toUser(request);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(normalizeRequired(request.getFullName()));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChangedAt(OffsetDateTime.now());
        User actor = currentActorOrNull();
        user.setCreatedBy(actor);
        user.setUpdatedBy(actor);

        return userMapper.toUserResponse(saveUser(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toUserResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"))
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User user = findUser(id);
        String username = normalizeRequired(request.getUsername());
        String email = normalizeRequired(request.getEmail());

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        userMapper.updateUser(user, request);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(normalizeRequired(request.getFullName()));
        user.setPhone(normalizeOptional(request.getPhone()));
        User actor = currentActorOrNull();
        if (actor != null) {
            user.setUpdatedBy(actor);
        }

        return userMapper.toUserResponse(saveUser(user));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Role findAssignableRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        if (role.getStatus() != RoleStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROLE_INACTIVE);
        }
        return role;
    }

    private User currentActorOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        UUID actorId;
        try {
            actorId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }
        return userRepository.findById(actorId)
                .orElseThrow(() -> new AppException(ErrorCode.AUTHENTICATED_USER_NOT_FOUND));
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
