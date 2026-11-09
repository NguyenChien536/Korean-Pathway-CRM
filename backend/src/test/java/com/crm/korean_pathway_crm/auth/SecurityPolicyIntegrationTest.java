package com.crm.korean_pathway_crm.auth;

import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.auth.repository.InvalidatedTokenRepository;
import com.crm.korean_pathway_crm.auth.repository.RefreshTokenRepository;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.permission.*;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.role.service.RoleService;
import com.crm.korean_pathway_crm.user.dto.request.UserUpdateRequest;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.crm.korean_pathway_crm.user.service.UserService;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
        "spring.datasource.url=${POLICY_TEST_DB_URL:jdbc:h2:mem:security_policy;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}")
@ActiveProfiles("test")
class SecurityPolicyIntegrationTest {
    private static final String PASSWORD = "Password1!";
    @Autowired private Environment environment;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private PermissionRepository permissions;
    @Autowired private RolePermissionRepository rolePermissions;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private InvalidatedTokenRepository invalidatedTokens;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private UserService userService;
    @Autowired private RoleService roleService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private String baseUrl;
    private Role adminRole;
    private Role staffRole;

    @BeforeEach
    void setUp() {
        // Class này có database riêng; không xóa dữ liệu của các test class khác.
        refreshTokens.deleteAllInBatch();
        invalidatedTokens.deleteAllInBatch();
        users.deleteAllInBatch();
        baseUrl = "http://localhost:" + environment.getRequiredProperty("local.server.port") + "/crm-korean";
        adminRole = role("SYSTEM_ADMIN");
        staffRole = role("POLICY_STAFF");
        grant(adminRole, "USERS.MANAGE");
    }

    @Test
    void delegatedManagerCannotResetOrDeactivateAdmin() throws Exception {
        Role delegatedRole = role("POLICY_MANAGER");
        grant(delegatedRole, "USERS.MANAGE");
        User manager = user(delegatedRole);
        User admin = user(adminRole);
        String access = login(manager);

        assertError(request("PUT", "/api/v1/users/" + admin.getId(),
                Map.of("password", "TakenOver123!", "confirmPassword", "TakenOver123!"), access), 403, 1006);
        assertError(request("DELETE", "/api/v1/users/" + admin.getId(), null, access), 403, 1006);
        User stored = users.findById(admin.getId()).orElseThrow();
        assertThat(stored.isActive()).isTrue();
        assertThat(encoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
    }

    @Test
    void delegatedManagerCanEditOrdinaryUserButCannotAssignRolesOrCreatePrivilegedUser() throws Exception {
        Role delegatedRole = role("POLICY_MANAGER");
        Role powerfulCustomRole = role("POLICY_POWERFUL");
        grant(delegatedRole, "USERS.MANAGE");
        grant(powerfulCustomRole, "CONFIGURATION.MANAGE");
        User manager = user(delegatedRole);
        User staff = user(staffRole);
        String access = login(manager);

        assertThat(request("PUT", "/api/v1/users/" + staff.getId(),
                Map.of("fullName", "Updated staff"), access).statusCode()).isEqualTo(200);
        for (User target : List.of(manager, staff)) {
            assertError(request("PUT", "/api/v1/users/" + target.getId(),
                    Map.of("roleId", powerfulCustomRole.getId().toString()), access), 403, 1006);
        }
        String username = "new-" + UUID.randomUUID();
        assertError(request("POST", "/api/v1/users", Map.of(
                "username", username, "email", username + "@example.test", "fullName", "New user",
                "password", PASSWORD, "confirmPassword", PASSWORD,
                "roleId", powerfulCustomRole.getId().toString()), access), 403, 1006);
        assertThat(users.existsByUsernameIgnoreCase(username)).isFalse();
        assertError(request("DELETE", "/api/v1/users/" + manager.getId(), null, access), 409, 1012);
    }

    @Test
    void lastActiveAdminCannotBeDemotedEvenWhenAnInactiveAdminExists() throws Exception {
        User admin = user(adminRole);
        User inactiveAdmin = user(adminRole);
        inactiveAdmin.setActive(false);
        users.saveAndFlush(inactiveAdmin);

        assertError(request("PUT", "/api/v1/users/" + admin.getId(),
                Map.of("roleId", staffRole.getId().toString()), login(admin)), 409, 1014);
        assertThat(users.findWithRoleById(admin.getId()).orElseThrow().getRole().getCode())
                .isEqualTo("SYSTEM_ADMIN");
    }

    @Test
    void adminCanAssignRoleAndResetAnotherAdminWhileSelfDeactivationRemainsForbidden() throws Exception {
        User actor = user(adminRole);
        User target = user(adminRole);
        String access = login(actor);

        assertThat(request("PUT", "/api/v1/users/" + target.getId(),
                Map.of("password", "ResetPassword2!", "confirmPassword", "ResetPassword2!"), access)
                .statusCode()).isEqualTo(200);
        assertThat(encoder.matches("ResetPassword2!", users.findById(target.getId()).orElseThrow().getPasswordHash()))
                .isTrue();
        assertError(request("DELETE", "/api/v1/users/" + actor.getId(), null, access), 409, 1012);
        assertThat(request("DELETE", "/api/v1/users/" + target.getId(), null, access).statusCode()).isEqualTo(200);
        assertThat(users.countActiveByRoleCode("SYSTEM_ADMIN")).isEqualTo(1);

        User staff = user(staffRole);
        assertThat(request("PUT", "/api/v1/users/" + staff.getId(),
                Map.of("roleId", adminRole.getId().toString()), access).statusCode()).isEqualTo(200);
        assertThat(users.countActiveByRoleCode("SYSTEM_ADMIN")).isEqualTo(2);
        String username = "created-" + UUID.randomUUID();
        assertThat(request("POST", "/api/v1/users", Map.of(
                "username", username, "email", username + "@example.test", "fullName", "Created user",
                "password", PASSWORD, "confirmPassword", PASSWORD,
                "roleId", staffRole.getId().toString()), access).statusCode()).isEqualTo(200);
    }

    @Test
    @Timeout(25)
    void concurrentAdminDemotionsWaitForSharedLockAndLeaveOneActiveAdmin() throws Exception {
        User first = user(adminRole);
        User second = user(adminRole);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futures = new java.util.ArrayList<>();
        CountDownLatch started = new CountDownLatch(2);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                roles.findByCodeForUpdate("SYSTEM_ADMIN").orElseThrow();
                for (User actor : List.of(first, second)) {
                    futures.add(executor.submit(() -> {
                        var context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(new UsernamePasswordAuthenticationToken(actor.getId().toString(), null,
                                List.of(new SimpleGrantedAuthority("USERS.MANAGE"))));
                        SecurityContextHolder.setContext(context);
                        started.countDown();
                        try {
                            userService.updateUser(actor.getId(), UserUpdateRequest.builder().roleId(staffRole.getId()).build());
                            return 0;
                        } catch (AppException exception) {
                            return exception.getErrorCode().getCode();
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    }));
                }
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                    for (Future<Integer> future : futures) {
                        // Khi khóa chung còn được giữ, cả hai mutation phải đợi.
                        assertThrows(TimeoutException.class, () -> future.get(150, TimeUnit.MILLISECONDS));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            });
            assertThat(List.of(futures.get(0).get(10, TimeUnit.SECONDS), futures.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(0, 1014);
            assertThat(users.countActiveByRoleCode("SYSTEM_ADMIN")).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(20)
    void staleProfileWriteDoesNotRestoreARoleChangedByAnotherTransaction() throws Exception {
        User actor = user(adminRole);
        User target = user(adminRole);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                User staleProfile = users.findById(target.getId()).orElseThrow();
                // Tải role vào snapshot trước khi transaction quản trị khác hạ role.
                assertThat(staleProfile.getRole().getCode()).isEqualTo("SYSTEM_ADMIN");
                Future<?> demotion = executor.submit(() -> {
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken(actor.getId().toString(), null,
                            List.of(new SimpleGrantedAuthority("USERS.MANAGE"))));
                    SecurityContextHolder.setContext(context);
                    try {
                        userService.updateUser(target.getId(), UserUpdateRequest.builder().roleId(staffRole.getId()).build());
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                try {
                    demotion.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                } catch (ExecutionException | TimeoutException exception) {
                    throw new IllegalStateException(exception);
                }
                staleProfile.setFullName("Changed after demotion");
                users.flush();
            });
            User stored = users.findWithRoleById(target.getId()).orElseThrow();
            assertThat(stored.getRole().getId()).isEqualTo(staffRole.getId());
            assertThat(stored.getFullName()).isEqualTo("Changed after demotion");
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"logout", "password", "inactive", "role", "locked", "expired", "issuer", "malformed"})
    void introspectionUsesSameSessionRulesAsProtectedApis(String invalidation) throws Exception {
        User caller = user(staffRole);
        User target = user(staffRole);
        String callerAccess = login(caller);
        String checkedToken = login(target);
        assertThat(result(request("POST", "/api/v1/auth/introspect", Map.of("token", checkedToken), callerAccess)))
                .containsEntry("valid", true);

        switch (invalidation) {
            case "logout" -> assertThat(request("POST", "/api/v1/auth/logout", Map.of(), checkedToken).statusCode()).isEqualTo(200);
            case "password" -> {
                target.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
                users.saveAndFlush(target);
            }
            case "inactive" -> {
                target.setActive(false);
                users.saveAndFlush(target);
            }
            case "role" -> {
                target.setRole(role("POLICY_OTHER"));
                users.saveAndFlush(target);
            }
            case "locked" -> {
                target.setLockedUntil(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15));
                users.saveAndFlush(target);
            }
            case "expired", "issuer" -> checkedToken = alterClaims(checkedToken, invalidation);
            case "malformed" -> checkedToken = "not-a-jwt";
        }
        assertThat(result(request("POST", "/api/v1/auth/introspect", Map.of("token", checkedToken), callerAccess)))
                .containsEntry("valid", false);
        assertThat(request("GET", "/api/v1/users/me", null, checkedToken).statusCode()).isEqualTo(401);
    }

    @Test
    void introspectValidatesBodyAndRequiresAuthenticatedCaller() throws Exception {
        String access = login(user(staffRole));
        for (Map<String, String> body : List.of(Map.<String, String>of(), Map.of("token", ""))) {
            assertError(request("POST", "/api/v1/auth/introspect", body, access), 400, 1010);
        }
        assertError(request("POST", "/api/v1/auth/introspect", Map.of("token", access), null), 401, 1011);
    }

    @Test
    void replaceRolePermissionsPreservesRetainedLinksAndIsIdempotent() throws Exception {
        String access = configurationAdminAccess();
        Role custom = role("REPLACE_" + UUID.randomUUID());
        grant(custom, "LEADS.READ");
        grant(custom, "LEADS.CREATE");
        grant(role("LOOKUP_" + UUID.randomUUID()), "LEADS.UPDATE");
        Permission read = permissions.findAll().stream().filter(p -> p.getCode().equals("LEADS.READ")).findFirst().orElseThrow();
        LocalDateTime originalTime = LocalDateTime.of(2020, 1, 1, 0, 0);
        jdbc.update("update role_permissions set created_at = TIMESTAMP '2020-01-01 00:00:00' where role_id = ? and permission_id = ?",
                custom.getId(), read.getId());

        var body = Map.of("permissionCodes", List.of("LEADS.READ", "LEADS.UPDATE", "LEADS.READ"));
        HttpResponse<String> changed = request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions", body, access);
        assertThat(changed.statusCode()).withFailMessage(changed.body()).isEqualTo(200);
        assertThat(json.readTree(changed.body()).get("result").size()).isEqualTo(2);
        assertThat(roles.findPermissionCodesByRoleId(custom.getId())).containsExactlyInAnyOrder("LEADS.READ", "LEADS.UPDATE");
        assertThat(rolePermissions.findById(new RolePermissionId(custom.getId(), read.getId())).orElseThrow().getCreatedAt())
                .isEqualTo(originalTime);
        LocalDateTime updatedAt = roles.findById(custom.getId()).orElseThrow().getUpdatedAt();

        assertThat(request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions", body, access).statusCode()).isEqualTo(200);
        assertThat(roles.findById(custom.getId()).orElseThrow().getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(rolePermissions.findAllByRole_Id(custom.getId())).hasSize(2);
        assertThat(request("GET", "/api/v1/roles/" + custom.getId() + "/permissions", null, access).statusCode()).isEqualTo(200);
    }

    @Test
    void missingPermissionCodeDoesNotPartiallyChangeRole() throws Exception {
        String access = configurationAdminAccess();
        Role custom = role("UNKNOWN_" + UUID.randomUUID());
        grant(custom, "LEADS.READ");
        LocalDateTime updatedAt = roles.findById(custom.getId()).orElseThrow().getUpdatedAt();

        assertError(request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions",
                Map.of("permissionCodes", List.of("LEADS.READ", "UNKNOWN.PERMISSION")), access), 400, 3001);
        assertThat(roles.findPermissionCodesByRoleId(custom.getId())).containsExactly("LEADS.READ");
        assertThat(roles.findById(custom.getId()).orElseThrow().getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void emptySetRevokesAllCustomRolePermissions() throws Exception {
        String access = configurationAdminAccess();
        Role custom = role("EMPTY_" + UUID.randomUUID());
        grant(custom, "LEADS.READ");
        HttpResponse<String> response = request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions",
                Map.of("permissionCodes", List.of()), access);
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        assertThat(json.readTree(response.body()).get("result").isArray()).isTrue();
        assertThat(json.readTree(response.body()).get("result").size()).isZero();
        assertThat(rolePermissions.findAllByRole_Id(custom.getId())).isEmpty();
    }

    @Test
    void permissionUpdateRejectsMissingNullBlankOrOversizedCodesAndMissingRole() throws Exception {
        String access = configurationAdminAccess();
        Role custom = role("VALIDATION_" + UUID.randomUUID());
        grant(custom, "LEADS.READ");
        String path = "/api/v1/roles/" + custom.getId() + "/permissions";
        Map<String, Object> nullCodes = new java.util.HashMap<>();
        nullCodes.put("permissionCodes", null);
        for (Object body : List.of(Map.of(), nullCodes,
                Map.of("permissionCodes", List.of(" ")),
                Map.of("permissionCodes", java.util.Arrays.asList((String) null)),
                Map.of("permissionCodes", List.of("A".repeat(101))),
                Map.of("permissionCodes", java.util.stream.IntStream.range(0, 101).mapToObj(i -> "CODE." + i).toList()))) {
            assertError(request("PUT", path, body, access), 400, 1010);
        }
        assertError(request("PUT", path, null, access), 400, 1010);
        assertError(request("PUT", "/api/v1/roles/" + UUID.randomUUID() + "/permissions",
                Map.of("permissionCodes", List.of()), access), 404, 2001);
        assertThat(roles.findPermissionCodesByRoleId(custom.getId())).containsExactly("LEADS.READ");
    }

    @Test
    void adminPermissionSetCannotBeChangedButIdenticalPutIsAllowed() throws Exception {
        String access = configurationAdminAccess();
        Set<String> original = roles.findPermissionCodesByRoleId(adminRole.getId());
        assertError(request("PUT", "/api/v1/roles/" + adminRole.getId() + "/permissions",
                Map.of("permissionCodes", List.of()), access), 409, 2007);
        assertThat(roles.findPermissionCodesByRoleId(adminRole.getId())).isEqualTo(original);
        assertThat(request("PUT", "/api/v1/roles/" + adminRole.getId() + "/permissions",
                Map.of("permissionCodes", original), access).statusCode()).isEqualTo(200);
    }

    @Test
    void onlySystemAdminWithConfigurationPermissionCanReplaceRolePermissions() throws Exception {
        Role custom = role("AUTH_" + UUID.randomUUID());
        Role delegated = role("DELEGATED_" + UUID.randomUUID());
        grant(delegated, "CONFIGURATION.MANAGE");
        String delegatedAccess = login(user(delegated));
        assertError(request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions",
                Map.of("permissionCodes", List.of()), delegatedAccess), 403, 1006);
        assertError(request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions",
                Map.of("permissionCodes", List.of()), null), 401, 1011);
    }

    @Test
    void grantedAndRevokedPermissionAffectsTheSameAccessTokenOnNextRequest() throws Exception {
        String adminAccess = configurationAdminAccess();
        Role custom = role("DYNAMIC_" + UUID.randomUUID());
        grant(role("CATALOG_" + UUID.randomUUID()), "USERS.READ");
        String userAccess = login(user(custom));
        assertError(request("GET", "/api/v1/users", null, userAccess), 403, 1006);
        assertThat(request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions",
                Map.of("permissionCodes", List.of("USERS.READ")), adminAccess).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/api/v1/users", null, userAccess).statusCode()).isEqualTo(200);
        assertThat(request("PUT", "/api/v1/roles/" + custom.getId() + "/permissions",
                Map.of("permissionCodes", List.of()), adminAccess).statusCode()).isEqualTo(200);
        assertError(request("GET", "/api/v1/users", null, userAccess), 403, 1006);
    }

    @Test
    @Timeout(25)
    void concurrentReplacementsWaitForRoleLockAndDoNotMergeTheirSets() throws Exception {
        grant(adminRole, "CONFIGURATION.MANAGE");
        Role custom = role("CONCURRENT_" + UUID.randomUUID());
        Role catalog = role("BATCH_" + UUID.randomUUID());
        grant(catalog, "LEADS.READ");
        grant(catalog, "LEADS.CREATE");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<?>> futures = new java.util.ArrayList<>();
        CountDownLatch started = new CountDownLatch(2);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                roles.findByIdForUpdate(custom.getId()).orElseThrow();
                for (String code : List.of("LEADS.READ", "LEADS.CREATE")) {
                    futures.add(executor.submit(() -> {
                        var context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(new UsernamePasswordAuthenticationToken("policy-test", null, List.of(
                                new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                                new SimpleGrantedAuthority("CONFIGURATION.MANAGE"))));
                        SecurityContextHolder.setContext(context);
                        started.countDown();
                        try {
                            roleService.updateRolePermissions(custom.getId(),
                                    com.crm.korean_pathway_crm.permission.dto.RolePermissionRequest.builder()
                                            .permissionCodes(Set.of(code)).build());
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    }));
                }
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                    for (Future<?> future : futures) {
                        assertThrows(TimeoutException.class, () -> future.get(150, TimeUnit.MILLISECONDS));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            });
            for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS);
            Set<String> result = roles.findPermissionCodesByRoleId(custom.getId());
            assertThat(result).hasSize(1);
            assertThat(result.iterator().next()).isIn("LEADS.READ", "LEADS.CREATE");
        } finally {
            executor.shutdownNow();
        }
    }

    private String configurationAdminAccess() throws Exception {
        grant(adminRole, "CONFIGURATION.MANAGE");
        return login(user(adminRole));
    }

    private Role role(String code) {
        return roles.findByCodeIgnoreCase(code).orElseGet(() -> roles.saveAndFlush(
                Role.builder().code(code).name(code).isSystem(code.equals("SYSTEM_ADMIN")).build()));
    }

    private void grant(Role role, String code) {
        Permission permission = permissions.findAll().stream().filter(p -> p.getCode().equals(code)).findFirst()
                .orElseGet(() -> {
                    Permission p = new Permission();
                    p.setCode(code);
                    p.setName(code);
                    return permissions.saveAndFlush(p);
                });
        RolePermissionId id = new RolePermissionId(role.getId(), permission.getId());
        if (!rolePermissions.existsById(id)) {
            RolePermission link = new RolePermission();
            link.setRolePermissionId(id);
            link.setRole(role);
            link.setPermission(permission);
            rolePermissions.saveAndFlush(link);
        }
    }

    private User user(Role role) {
        String suffix = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return users.saveAndFlush(User.builder().username(suffix).email(suffix + "@example.test")
                .fullName("Policy user").passwordHash(encoder.encode(PASSWORD)).role(role)
                .createdAt(now).updatedAt(now).build());
    }

    private String login(User user) throws Exception {
        return (String) result(request("POST", "/api/v1/auth/token",
                Map.of("username", user.getUsername(), "password", PASSWORD), null)).get("accessToken");
    }

    private String alterClaims(String token, String kind) throws Exception {
        SignedJWT original = SignedJWT.parse(token);
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder(original.getJWTClaimsSet());
        if (kind.equals("expired")) {
            claims.issueTime(Date.from(Instant.now().minusSeconds(120)))
                    .expirationTime(Date.from(Instant.now().minusSeconds(60)));
        } else {
            claims.issuer("untrusted-issuer");
        }
        SignedJWT altered = new SignedJWT(original.getHeader(), claims.build());
        altered.sign(new MACSigner(jwtProperties.signingKeyBytes()));
        return altered.serialize();
    }

    private HttpResponse<String> request(String method, String path, Object body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
        if (body != null) {
            builder.header("Content-Type", "application/json");
            publisher = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
        }
        return client.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> result(HttpResponse<String> response) {
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        return (Map<String, Object>) json.readValue(response.body(), Map.class).get("result");
    }

    private void assertError(HttpResponse<String> response, int status, int code) {
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(status);
        assertThat(json.readValue(response.body(), Map.class)).containsEntry("code", code);
    }
}
