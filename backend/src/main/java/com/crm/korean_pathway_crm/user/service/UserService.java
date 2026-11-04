package com.crm.korean_pathway_crm.user.service;

import com.crm.korean_pathway_crm.auth.service.RefreshTokenService;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.user.dto.request.*;
import com.crm.korean_pathway_crm.user.dto.response.UserResponse;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.mapper.UserMapper;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.crm.korean_pathway_crm.user.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final String USERNAME_UNIQUE_INDEX = "uq_users_username_lower";
    private static final String EMAIL_UNIQUE_INDEX = "uq_users_email_lower";
    private static final String PHONE_UNIQUE_INDEX = "uq_users_phone";
    private static final Map<String, String> USER_SORT_FIELDS = Map.of(
            "username", "username",
            "fullName", "fullName",
            "email", "email",
            "createdAt", "createdAt",
            "status", "isActive"
    );

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
    public Page<UserResponse> getAllUsers(UserFilterRequest request) {
        log.debug("Retrieving users in the system with pagination and filters");

        // Kiểm tra quyền ở service để mọi nơi gọi method này đều phải có quyền, không chỉ riêng controller.
        requirePermission("USERS.READ");

        // Mặc định sắp xếp là quy tắc riêng của danh sách User, nên đặt ở đây thay vì lớp request dùng chung.
        String sortBy = StringUtils.hasText(request.getSortBy())
                ? request.getSortBy().trim()
                : "username";

        // Chỉ chấp nhận field nằm trong danh sách cho phép, không đưa trực tiếp tên field tùy ý từ client vào JPA.
        // Tên "status" trên API được ánh xạ sang thuộc tính "isActive" của entity User.
        String entityField = USER_SORT_FIELDS.get(sortBy);

        if (entityField == null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }

        // Chuyển chiều sắp xếp từ API sang kiểu Spring Data; mặc định đã thống nhất là tăng dần (ASC).
        Sort.Direction sortDir = Sort.Direction.ASC;
        if (StringUtils.hasText(request.getSortDir())) {
            try {
                sortDir = Sort.Direction.fromString(request.getSortDir().trim());
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.INVALID_PARAMETER);
            }
        }

        // Thêm id làm tiêu chí phụ để các user trùng giá trị sort chính vẫn có thứ tự xác định.
        Sort sort = Sort.by(
                new Sort.Order(sortDir, entityField),
                Sort.Order.asc("id")
        );

        // Pageable mô tả trang, số dòng tối đa và thứ tự; nó không chứa điều kiện search/filter.
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );

        // Specification tạo các điều kiện WHERE tùy chọn; repository áp dụng chúng trước khi phân trang.
        Specification<User> spec = UserSpecification.of(request);
        Page<User> page = userRepository.findAll(spec, pageable);

        // Chỉ chuyển các entity thuộc trang hiện tại sang DTO an toàn; Page.map vẫn giữ metadata phân trang.
        return page.map(userMapper::toUserResponse);
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

    @Transactional(readOnly = true)
    public UserResponse getMyInfo() {
        User user = currentActor();
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateMyInfo(MyInfoUpdateRequest request) {
        User user = currentActor();
        if (request.getUsername() != null) {
            String username = normalizeRequired(request.getUsername());
            if (username.length() < 2) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            ensureUsernameAvailable(username, user.getId());
            user.setUsername(username);
        }

        if (request.getEmail() != null) {
            String email = normalizeRequired(request.getEmail());
            ensureEmailAvailable(email, user.getId());
            user.setEmail(email);
        }

        if (request.getPhone() != null) {
            String phone = normalizeOptional(request.getPhone());
            ensurePhoneAvailable(phone, user.getId());
            user.setPhone(phone);
        }

        user.setUpdatedBy(user);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return userMapper.toUserResponse(saveUser(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentActor();
        ensureBcryptPasswordLength(request.getOldPassword());
        ensureBcryptPasswordLength(request.getNewPassword());
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }
        ensurePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setUpdatedBy(user);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        saveUser(user);
        refreshTokenService.revokeAllUserRefreshTokens(user.getUsername());
        log.info("Password changed successfully for user ID: {}", user.getId());
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User actor = requirePermission("USERS.MANAGE");
        User user = findUser(userId);

        // Keep audit and business-history references intact; disable the account instead of deleting it.
        if (actor.getId().equals(user.getId())) {
            throw new AppException(ErrorCode.CANNOT_DEACTIVATE_SELF);
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

    private void ensureBcryptPasswordLength(String password) {
        // BCrypt limits the UTF-8 byte length, while @Size counts Java characters.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
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
