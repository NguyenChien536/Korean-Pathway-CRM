package com.crm.korean_pathway_crm.auth;

import com.crm.korean_pathway_crm.auth.repository.RefreshTokenRepository;
import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.auth.entity.InvalidatedToken;
import com.crm.korean_pathway_crm.auth.repository.InvalidatedTokenRepository;
import com.crm.korean_pathway_crm.scheduler.TokenCleanupScheduler;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTest {
    @Autowired private Environment environment;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private InvalidatedTokenRepository invalidatedTokens;
    @Autowired private TokenCleanupScheduler cleanupScheduler;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private User user;
    private Role role;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + environment.getRequiredProperty("local.server.port") + "/crm-korean";
        String suffix = UUID.randomUUID().toString();
        role = roles.saveAndFlush(Role.builder().code("COUNSELOR_" + suffix).name("Counselor").build());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user = users.saveAndFlush(User.builder()
                .username("test-" + suffix).email(suffix + "@example.com").fullName("Test User")
                .passwordHash(encoder.encode("Password1!")).role(role).createdAt(now).updatedAt(now).build());
    }

    @Test
    void loginAndRefreshReturnSameResponseContractAndRotateToken() throws Exception {
        HttpResponse<String> login = login();
        Map<String, Object> original = result(login);
        assertTokenResponse(login, original);

        String oldRefresh = (String) original.get("refreshToken");
        HttpResponse<String> refreshed = post("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh), null);
        Map<String, Object> next = result(refreshed);
        assertTokenResponse(refreshed, next);

        assertThat(next.keySet()).isEqualTo(original.keySet());
        assertThat(next.get("accessToken")).isNotEqualTo(original.get("accessToken"));
        assertThat(next.get("refreshToken")).isNotEqualTo(oldRefresh);
        assertThat(refreshTokens.findByToken(oldRefresh).orElseThrow().isRevoked()).isTrue();
        assertThat(getInfo((String) next.get("accessToken")).statusCode()).isEqualTo(200);
        assertThat(post("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh), null).statusCode()).isEqualTo(401);
    }

    @Test
    void missingBlankUnknownAndExpiredRefreshTokensAreRejected() throws Exception {
        assertThat(post("/api/v1/auth/refresh", Map.of(), null).statusCode()).isEqualTo(400);
        assertThat(post("/api/v1/auth/refresh", Map.of("refreshToken", " "), null).statusCode()).isEqualTo(400);
        assertThat(post("/api/v1/auth/refresh", Map.of("refreshToken", "unknown"), null).statusCode()).isEqualTo(401);

        String token = (String) result(login()).get("refreshToken");
        var stored = refreshTokens.findByToken(token).orElseThrow();
        stored.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        refreshTokens.saveAndFlush(stored);
        assertThat(post("/api/v1/auth/refresh", Map.of("refreshToken", token), null).statusCode()).isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactive-user", "inactive-role", "locked-user"})
    void refreshChecksCurrentAccountAndRoleState(String state) throws Exception {
        String token = (String) result(login()).get("refreshToken");
        if (state.equals("inactive-role")) {
            role.setStatus(RoleStatus.INACTIVE);
            roles.saveAndFlush(role);
        } else {
            if (state.equals("inactive-user")) {
                user.setActive(false);
            } else {
                user.setLockedUntil(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15));
            }
            users.saveAndFlush(user);
        }
        assertThat(post("/api/v1/auth/refresh", Map.of("refreshToken", token), null).statusCode()).isEqualTo(401);
    }

    @Test
    void concurrentRefreshOnlyAllowsOneUseOfOldToken() throws Exception {
        String oldToken = (String) result(login()).get("refreshToken");
        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> refreshStatus(oldToken));
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> refreshStatus(oldToken));

        assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 401);
    }

    @Test
    void logoutRevokesBothTokensAndAccessTokenStopsWorkingImmediately() throws Exception {
        Map<String, Object> tokens = result(login());
        String access = (String) tokens.get("accessToken");
        String refresh = (String) tokens.get("refreshToken");
        assertThat(getInfo(access).statusCode()).isEqualTo(200);

        assertThat(post("/api/v1/auth/logout", Map.of("refreshToken", refresh), access).statusCode()).isEqualTo(200);

        assertThat(getInfo(access).statusCode()).isEqualTo(401);
        assertThat(post("/api/v1/auth/refresh", Map.of("refreshToken", refresh), null).statusCode()).isEqualTo(401);
        assertThat(post("/api/v1/auth/logout", Map.of("refreshToken", refresh), null).statusCode()).isEqualTo(200);
        assertThat(post("/api/v1/auth/logout", Map.of("refreshToken", refresh), access).statusCode()).isEqualTo(200);
    }

    @Test
    void refreshAndLogoutWorkWhenClientStillSendsExpiredAccessToken() throws Exception {
        Map<String, Object> tokens = result(login());
        String expiredAccess = signedAccessToken(Instant.now().minusSeconds(1));
        HttpResponse<String> refresh = post("/api/v1/auth/refresh",
                Map.of("refreshToken", (String) tokens.get("refreshToken")), expiredAccess);
        assertThat(refresh.statusCode()).isEqualTo(200);
        String rotatedRefresh = (String) result(refresh).get("refreshToken");

        assertThat(post("/api/v1/auth/logout", Map.of("refreshToken", rotatedRefresh), expiredAccess).statusCode())
                .isEqualTo(200);
        assertThat(post("/api/v1/auth/refresh", Map.of("refreshToken", rotatedRefresh), null).statusCode())
                .isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactive-user", "inactive-role", "locked-user", "changed-password", "changed-role"})
    void issuedAccessTokenIsRejectedAfterAccountCredentialsOrRoleChange(String state) throws Exception {
        String access = (String) result(login()).get("accessToken");
        if (state.equals("inactive-role")) {
            role.setStatus(RoleStatus.INACTIVE);
            roles.saveAndFlush(role);
        } else {
            switch (state) {
                case "inactive-user" -> user.setActive(false);
                case "locked-user" -> user.setLockedUntil(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15));
                case "changed-password" -> user.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
                case "changed-role" -> user.setRole(roles.saveAndFlush(Role.builder()
                        .code("OTHER_" + UUID.randomUUID()).name("Other").build()));
            }
            users.saveAndFlush(user);
        }
        assertThat(getInfo(access).statusCode()).isEqualTo(401);
    }

    @Test
    void failedLoginsAreCommittedAndExpiredLockStartsANewAttemptWindow() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(post("/api/v1/auth/log-in", Map.of("username", user.getUsername(), "password", "Wrong1!"), null)
                    .statusCode()).isEqualTo(401);
        }
        User locked = users.findById(user.getId()).orElseThrow();
        assertThat(locked.getFailedLoginCount()).isEqualTo(5);
        assertThat(locked.getLockedUntil()).isAfter(LocalDateTime.now(ZoneOffset.UTC));
        assertThat(login().statusCode()).isEqualTo(401);

        locked.setLockedUntil(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        users.saveAndFlush(locked);
        assertThat(post("/api/v1/auth/log-in", Map.of("username", user.getUsername(), "password", "Wrong1!"), null)
                .statusCode()).isEqualTo(401);
        User afterUnlock = users.findById(user.getId()).orElseThrow();
        assertThat(afterUnlock.getFailedLoginCount()).isEqualTo(1);
        assertThat(afterUnlock.getLockedUntil()).isNull();
        assertThat(login().statusCode()).isEqualTo(200);
    }

    @Test
    void cleanupDeletesExpiredBlacklistEntriesAndPreservesActiveOnes() {
        String expiredId = UUID.randomUUID().toString();
        String activeId = UUID.randomUUID().toString();
        invalidatedTokens.saveAndFlush(InvalidatedToken.builder().id(expiredId)
                .expiryTime(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)).build());
        invalidatedTokens.saveAndFlush(InvalidatedToken.builder().id(activeId)
                .expiryTime(LocalDateTime.now(ZoneOffset.UTC).plusHours(1)).build());

        cleanupScheduler.cleanExpiredInvalidatedTokens();

        assertThat(invalidatedTokens.existsById(expiredId)).isFalse();
        assertThat(invalidatedTokens.existsById(activeId)).isTrue();
    }

    @Test
    void malformedRequestAndMissingBearerReturnConsistentApiErrors() throws Exception {
        HttpResponse<String> missingAccess = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/info"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(missingAccess.statusCode()).isEqualTo(401);
        assertThat(json.readValue(missingAccess.body(), Map.class)).containsEntry("code", 1011);

        HttpResponse<String> malformed = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/refresh"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(malformed.statusCode()).isEqualTo(400);
        assertThat(json.readValue(malformed.body(), Map.class)).containsEntry("code", 1010);
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "wrong-issuer", "missing-exp"})
    void accessTokenValidationKeepsExpiryAndIssuerChecksAlongsideBlacklist(String invalidClaim) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(invalidClaim.equals("wrong-issuer") ? "other-issuer" : jwtProperties.getIssuer())
                .subject(user.getId().toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now().minusSeconds(10)));
        if (!invalidClaim.equals("missing-exp")) {
            claims.expirationTime(Date.from(invalidClaim.equals("expired")
                    ? Instant.now().minusSeconds(1) : Instant.now().plusSeconds(3600)));
        }
        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims.build());
        token.sign(new MACSigner(jwtProperties.getSignerKey().getBytes(StandardCharsets.UTF_8)));

        assertThat(getInfo(token.serialize()).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> login() throws Exception {
        return post("/api/v1/auth/log-in", Map.of("username", user.getUsername(), "password", "Password1!"), null);
    }

    @Test
    void regularUserCannotCreateRoleAndHttpErrorsUseApiResponse() throws Exception {
        String access = (String) result(login()).get("accessToken");
        HttpResponse<String> denied = post("/api/v1/roles", Map.of("code", "CUSTOM", "name", "Custom"), access);
        assertThat(denied.statusCode()).withFailMessage(denied.body()).isEqualTo(403);
        assertThat(json.readValue(denied.body(), Map.class)).containsEntry("code", 1006);

        HttpResponse<String> invalidId = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/users/invalid-uuid"))
                .header("Authorization", "Bearer " + access).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(invalidId.statusCode()).isEqualTo(400);
        assertThat(json.readValue(invalidId.body(), Map.class)).containsEntry("code", 9003);

        HttpResponse<String> missing = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/roles/" + UUID.randomUUID()))
                .header("Authorization", "Bearer " + access).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(json.readValue(missing.body(), Map.class)).containsEntry("code", 2001);
    }

    @Test
    void adminGetsSpecificErrorsForDuplicateProtectedAndAssignedRoles() throws Exception {
        Role admin = roles.findAll().stream().filter(value -> value.getCode().equals("SYSTEM_ADMIN")).findFirst()
                .orElseGet(() -> roles.saveAndFlush(Role.builder().code("SYSTEM_ADMIN").name("Admin").isSystem(true).build()));
        user.setRole(admin);
        users.saveAndFlush(user);
        String access = (String) result(login()).get("accessToken");
        String code = "custom_" + UUID.randomUUID().toString().replace("-", "");

        HttpResponse<String> created = post("/api/v1/roles", Map.of("code", code, "name", " Custom role "), access);
        assertThat(created.statusCode()).withFailMessage(created.body()).isEqualTo(201);
        Map<String, Object> roleBody = json.readValue(created.body(), Map.class);
        assertThat(roleBody.get("code")).isEqualTo(code.toUpperCase(java.util.Locale.ROOT));
        assertThat(roleBody.get("name")).isEqualTo("Custom role");
        String roleId = (String) roleBody.get("id");
        HttpResponse<String> duplicate = post("/api/v1/roles", Map.of("code", code, "name", "Duplicate"), access);
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(json.readValue(duplicate.body(), Map.class)).containsEntry("code", 2004);

        HttpResponse<String> protectedRole = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/roles/" + admin.getId()))
                .header("Authorization", "Bearer " + access).DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(protectedRole.statusCode()).isEqualTo(409);
        assertThat(json.readValue(protectedRole.body(), Map.class)).containsEntry("code", 2005);

        Role assignedRole = roles.findById(UUID.fromString(roleId)).orElseThrow();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String suffix = UUID.randomUUID().toString();
        users.saveAndFlush(User.builder().username(suffix).email(suffix + "@example.com").fullName("Assigned user")
                .passwordHash(encoder.encode("Password1!")).role(assignedRole).createdAt(now).updatedAt(now).build());
        HttpResponse<String> assigned = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/roles/" + roleId))
                .header("Authorization", "Bearer " + access).DELETE().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(assigned.statusCode()).isEqualTo(409);
        assertThat(json.readValue(assigned.body(), Map.class)).containsEntry("code", 2003);
    }

    private String signedAccessToken(Instant expiry) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), new JWTClaimsSet.Builder()
                .issuer(jwtProperties.getIssuer()).subject(user.getId().toString())
                .jwtID(UUID.randomUUID().toString()).issueTime(Date.from(Instant.now().minusSeconds(60)))
                .expirationTime(Date.from(expiry)).build());
        jwt.sign(new MACSigner(jwtProperties.signingKeyBytes()));
        return jwt.serialize();
    }

    private int refreshStatus(String token) {
        try {
            return post("/api/v1/auth/refresh", Map.of("refreshToken", token), null).statusCode();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private HttpResponse<String> post(String path, Map<String, String> body, String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getInfo(String accessToken) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/info"))
                .header("Authorization", "Bearer " + accessToken).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> result(HttpResponse<String> response) {
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
        Map<String, Object> envelope = json.readValue(response.body(), Map.class);
        return (Map<String, Object>) envelope.get("result");
    }

    private void assertTokenResponse(HttpResponse<String> response, Map<String, Object> tokens) throws Exception {
        assertThat(tokens.keySet()).containsExactlyInAnyOrder(
                "authenticated", "tokenType", "accessToken", "refreshToken", "expiresIn", "accessTokenExpiresAt");
        assertThat(tokens.get("authenticated")).isEqualTo(true);
        assertThat(tokens.get("tokenType")).isEqualTo("Bearer");
        assertThat(((Number) tokens.get("expiresIn")).longValue()).isEqualTo(3600);
        var claims = SignedJWT.parse((String) tokens.get("accessToken")).getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("crm-test");
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(Instant.parse((String) tokens.get("accessTokenExpiresAt")))
                .isEqualTo(claims.getExpirationTime().toInstant());
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(claims.getIssueTime().toInstant().plusSeconds(3600));
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Pragma").orElseThrow()).isEqualTo("no-cache");
    }
}
