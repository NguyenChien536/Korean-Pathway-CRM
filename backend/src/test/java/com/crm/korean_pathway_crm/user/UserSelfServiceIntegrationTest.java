package com.crm.korean_pathway_crm.user;

import com.crm.korean_pathway_crm.auth.repository.RefreshTokenRepository;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserSelfServiceIntegrationTest {
    private static final String OLD_PASSWORD = "Password1!";
    private static final String NEW_PASSWORD = "ChangedPassword2!";
    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(10000000);

    @Autowired private Environment environment;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private PasswordEncoder encoder;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private String baseUrl;
    private User user;
    private Role role;
    private String access;
    private String refresh;
    private LocalDateTime originalTimestamp;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + environment.getRequiredProperty("local.server.port") + "/crm-korean";
        role = roles.saveAndFlush(Role.builder().code("SELF_" + UUID.randomUUID()).name("Counselor").build());
        originalTimestamp = LocalDateTime.now(ZoneOffset.UTC).minusDays(1).withNano(0);
        user = createUser();
        Map<String, Object> tokens = result(login(user.getUsername(), OLD_PASSWORD));
        access = (String) tokens.get("accessToken");
        refresh = (String) tokens.get("refreshToken");
    }

    @Test
    void emailOnlyUpdatePreservesOtherFieldsAndRecordsAuditWithoutManagementPermission() throws Exception {
        String email = "updated-" + user.getEmail();
        Map<String, Object> response = result(patch(Map.of("email", email)));
        assertThat(response).containsEntry("email", email).containsEntry("id", user.getId().toString())
                .containsEntry("updatedById", user.getId().toString());
        assertThat(response).doesNotContainKeys("passwordHash", "password", "accessToken", "refreshToken");

        User stored = storedUser();
        assertThat(stored.getUsername()).isEqualTo(user.getUsername());
        assertThat(stored.getPhone()).isEqualTo(user.getPhone());
        assertThat(stored.getFullName()).isEqualTo(user.getFullName());
        assertThat(stored.getRole().getId()).isEqualTo(role.getId());
        assertThat(stored.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(stored.getPasswordChangedAt()).isEqualTo(originalTimestamp);
        assertThat(stored.getCreatedAt()).isEqualTo(originalTimestamp);
        assertThat(stored.getCreatedBy()).isNull();
        assertThat(stored.getUpdatedBy().getId()).isEqualTo(user.getId());
        assertThat(stored.getUpdatedAt()).isAfter(originalTimestamp);
    }

    @Test
    void phoneOnlyUpdatePreservesEmailAndUsername() throws Exception {
        String phone = nextPhone();
        assertThat(result(patch(Map.of("phone", phone)))).containsEntry("phone", phone);
        assertThat(storedUser().getEmail()).isEqualTo(user.getEmail());
        assertThat(storedUser().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    void usernameUpdateTrimsValueAndChangesTheLoginName() throws Exception {
        String username = "renamed-" + UUID.randomUUID();
        assertThat(result(patch(Map.of("username", "  " + username + "  ")))).containsEntry("username", username);
        assertThat(storedUser().getEmail()).isEqualTo(user.getEmail());
        assertThat(login(username, OLD_PASSWORD).statusCode()).isEqualTo(200);
        assertError(login(user.getUsername(), OLD_PASSWORD), 401, 1008);
        assertThat(request("GET", "/api/v1/users/me", null, access).statusCode()).isEqualTo(200);
    }

    @Test
    void sameIdentifiersAndCaseChangesDoNotConflictWithTheCurrentUser() throws Exception {
        String username = user.getUsername().toUpperCase(java.util.Locale.ROOT);
        String email = user.getEmail().toUpperCase(java.util.Locale.ROOT);
        assertThat(result(patch(Map.of("username", username, "email", email, "phone", user.getPhone()))))
                .containsEntry("username", username).containsEntry("email", email);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingOrNullFieldsKeepTheirExistingValues(boolean explicitNull) throws Exception {
        Map<String, Object> body = new HashMap<>();
        if (explicitNull) {
            body.put("username", null);
            body.put("email", null);
            body.put("phone", null);
        }
        result(patch(body));
        User stored = storedUser();
        assertThat(stored.getUsername()).isEqualTo(user.getUsername());
        assertThat(stored.getEmail()).isEqualTo(user.getEmail());
        assertThat(stored.getPhone()).isEqualTo(user.getPhone());
    }

    @Test
    void emptyPhoneClearsTheStoredPhone() throws Exception {
        result(patch(Map.of("phone", "")));
        assertThat(storedUser().getPhone()).isNull();
        assertThat(storedUser().getEmail()).isEqualTo(user.getEmail());
    }

    @ParameterizedTest
    @CsvSource({"username,1001", "email,1002", "phone,1007"})
    void identifiersOwnedByAnotherUserAreRejected(String field, int code) throws Exception {
        User other = createUser();
        String value = switch (field) {
            case "username" -> other.getUsername().toUpperCase(java.util.Locale.ROOT);
            case "email" -> other.getEmail().toUpperCase(java.util.Locale.ROOT);
            default -> other.getPhone();
        };
        assertError(patch(Map.of(field, value)), 409, code);
        assertThat(storedUser().getUsername()).isEqualTo(user.getUsername());
        assertThat(storedUser().getEmail()).isEqualTo(user.getEmail());
        assertThat(storedUser().getPhone()).isEqualTo(user.getPhone());
    }

    @Test
    void duplicateEmailRollsBackAnEarlierUsernameChange() throws Exception {
        User other = createUser();
        assertError(patch(Map.of("username", "renamed-" + UUID.randomUUID(), "email", other.getEmail())), 409, 1002);
        User stored = storedUser();
        assertThat(stored.getUsername()).isEqualTo(user.getUsername());
        assertThat(stored.getEmail()).isEqualTo(user.getEmail());
        assertThat(stored.getUpdatedAt()).isEqualTo(originalTimestamp);
        assertThat(stored.getUpdatedBy()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"username\":\"\"}", "{\"username\":\"   \"}", "{\"username\":\" a \"}",
            "{\"email\":\"\"}", "{\"email\":\"not-an-email\"}", "{\"phone\":\"invalid\"}"
    })
    void invalidProfileFieldsAreRejected(String body) throws Exception {
        assertError(request("PATCH", "/api/v1/users/me", body, access), 400, 1010);
        assertThat(storedUser().getUpdatedAt()).isEqualTo(originalTimestamp);
    }

    @Test
    void changePasswordRecordsAuditAndInvalidatesAllExistingSessions() throws Exception {
        Map<String, Object> otherSession = result(login(user.getUsername(), OLD_PASSWORD));
        String otherAccess = (String) otherSession.get("accessToken");
        String otherRefresh = (String) otherSession.get("refreshToken");
        assertThat(changePassword(passwordBody()).statusCode()).isEqualTo(200);

        User stored = storedUser();
        assertThat(encoder.matches(NEW_PASSWORD, stored.getPasswordHash())).isTrue();
        assertThat(encoder.matches(OLD_PASSWORD, stored.getPasswordHash())).isFalse();
        assertThat(stored.getPasswordChangedAt()).isAfter(originalTimestamp);
        assertThat(stored.getUpdatedAt()).isAfterOrEqualTo(stored.getPasswordChangedAt());
        assertThat(stored.getUpdatedBy().getId()).isEqualTo(user.getId());
        assertThat(stored.getCreatedAt()).isEqualTo(originalTimestamp);
        assertThat(refreshTokens.findByToken(refresh).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokens.findByToken(otherRefresh).orElseThrow().isRevoked()).isTrue();
        assertError(request("GET", "/api/v1/users/me", null, access), 401, 1011);
        assertError(request("GET", "/api/v1/users/me", null, otherAccess), 401, 1011);
        assertError(request("POST", "/api/v1/auth/refresh", json.writeValueAsString(Map.of("refreshToken", refresh)), null), 401, 1009);
        assertError(login(user.getUsername(), OLD_PASSWORD), 401, 1008);
        assertThat(login(user.getUsername(), NEW_PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void wrongCurrentPasswordDoesNotChangeCredentialsOrRevokeTokens() throws Exception {
        Map<String, String> body = passwordBody();
        body.put("oldPassword", "WrongPassword1!");
        assertError(changePassword(body), 400, 1013);
        assertCredentialsUnchanged();
    }

    @Test
    void mismatchingConfirmationDoesNotChangeCredentialsOrRevokeTokens() throws Exception {
        Map<String, String> body = passwordBody();
        body.put("confirmPassword", "DifferentPassword3!");
        assertError(changePassword(body), 400, 1004);
        assertCredentialsUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"oldPassword", "newPassword", "confirmPassword"})
    void passwordFieldsMustBePresentAndNonBlank(String field) throws Exception {
        Map<String, String> body = passwordBody();
        body.remove(field);
        assertError(changePassword(body), 400, 1010);
        body.put(field, "   ");
        assertError(changePassword(body), 400, 1010);
        assertCredentialsUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"oldPassword", "newPassword"})
    void passwordsOverBcryptByteLimitAreRejectedWithoutChangingCredentials(String field) throws Exception {
        Map<String, String> body = passwordBody();
        String password = "Aa1!" + "é".repeat(35);
        body.put(field, password);
        if (field.equals("newPassword")) {
            body.put("confirmPassword", password);
        }
        assertError(changePassword(body), 400, 1010);
        assertCredentialsUnchanged();
    }

    @Test
    void unicodePasswordWithinBcryptByteLimitIsAccepted() throws Exception {
        String password = "Aa1!" + "é".repeat(34);
        Map<String, String> body = passwordBody();
        body.put("newPassword", password);
        body.put("confirmPassword", password);
        assertThat(changePassword(body).statusCode()).isEqualTo(200);
        assertThat(login(user.getUsername(), password).statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @CsvSource({"PATCH,/api/v1/users/me", "POST,/api/v1/users/change-password"})
    void personalAccountEndpointsRequireAuthentication(String method, String path) throws Exception {
        assertError(request(method, path, "{}", null), 401, 1011);
    }

    private User createUser() {
        String suffix = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().username("self-" + suffix).email(suffix + "@example.com")
                .fullName("Staff Name").phone(nextPhone()).passwordHash(encoder.encode(OLD_PASSWORD)).role(role)
                .createdAt(originalTimestamp).updatedAt(originalTimestamp).passwordChangedAt(originalTimestamp).build());
    }

    private String nextPhone() {
        return "09" + PHONE_SEQUENCE.getAndIncrement();
    }

    private User storedUser() {
        return users.findById(user.getId()).orElseThrow();
    }

    private void assertCredentialsUnchanged() throws Exception {
        User stored = storedUser();
        assertThat(stored.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(stored.getPasswordChangedAt()).isEqualTo(originalTimestamp);
        assertThat(stored.getUpdatedAt()).isEqualTo(originalTimestamp);
        assertThat(refreshTokens.findByToken(refresh).orElseThrow().isRevoked()).isFalse();
        assertThat(request("GET", "/api/v1/users/me", null, access).statusCode()).isEqualTo(200);
    }

    private Map<String, String> passwordBody() {
        return new HashMap<>(Map.of("oldPassword", OLD_PASSWORD, "newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD));
    }

    private HttpResponse<String> patch(Map<String, ?> body) throws Exception {
        return request("PATCH", "/api/v1/users/me", json.writeValueAsString(body), access);
    }

    private HttpResponse<String> changePassword(Map<String, String> body) throws Exception {
        return request("POST", "/api/v1/users/change-password", json.writeValueAsString(body), access);
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return request("POST", "/api/v1/auth/log-in", json.writeValueAsString(Map.of("username", username, "password", password)), null);
    }

    private HttpResponse<String> request(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
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
