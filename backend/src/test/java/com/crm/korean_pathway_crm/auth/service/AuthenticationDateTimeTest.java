package com.crm.korean_pathway_crm.auth.service;

import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.auth.dto.AuthenticationRequest;
import com.crm.korean_pathway_crm.auth.dto.AuthenticationResponse;
import com.crm.korean_pathway_crm.auth.entity.InvalidatedToken;
import com.crm.korean_pathway_crm.auth.entity.RefreshToken;
import com.crm.korean_pathway_crm.auth.repository.InvalidatedTokenRepository;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationDateTimeTest {
    private static final String TEST_SIGNING_KEY = "0123456789abcdef".repeat(4);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSignerKey(TEST_SIGNING_KEY);
        service = new AuthenticationService(properties, userRepository, passwordEncoder,
                refreshTokenService, invalidatedTokenRepository);
    }

    @Test
    void loginKeepsJwtEpochTimesConsistentWithUtcLocalLoginTime() throws Exception {
        Role role = Role.builder().id(UUID.randomUUID()).code("COUNSELOR").status(RoleStatus.ACTIVE).build();
        User user = User.builder().id(UUID.randomUUID()).username("counselor")
                .passwordHash("stored-hash").role(role).build();
        when(userRepository.findByUsernameIgnoreCaseForUpdate("counselor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "stored-hash")).thenReturn(true);
        when(refreshTokenService.generateRefreshToken(user)).thenReturn(
                RefreshToken.builder().token("refresh-token").build());

        Instant beforeLogin = Instant.now();
        AuthenticationResponse response = service.authenticate(
                AuthenticationRequest.builder().username("counselor").password("password").build());
        Instant afterLogin = Instant.now();

        Instant loginInstant = user.getLastLoginAt().toInstant(ZoneOffset.UTC);
        JWTClaimsSet claims = SignedJWT.parse(response.getAccessToken()).getJWTClaimsSet();
        assertThat(loginInstant).isBetween(beforeLogin, afterLogin);
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(loginInstant.truncatedTo(ChronoUnit.SECONDS));
        assertThat(response.getAccessTokenExpiresAt()).isEqualTo(loginInstant.truncatedTo(ChronoUnit.SECONDS).plus(60, ChronoUnit.MINUTES));
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600);
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(
                response.getAccessTokenExpiresAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void refreshReturnsSameTokenFieldsAsLogin() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).role(
                Role.builder().id(UUID.randomUUID()).code("COUNSELOR").status(RoleStatus.ACTIVE).build()).build();
        when(refreshTokenService.rotateAndGetNewToken("old-refresh")).thenReturn(
                RefreshToken.builder().token("new-refresh").user(user).build());

        AuthenticationResponse response = service.refreshToken("old-refresh");
        JWTClaimsSet claims = SignedJWT.parse(response.getAccessToken()).getJWTClaimsSet();

        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(response.getExpiresIn()).isEqualTo(3600);
        assertThat(response.getAccessTokenExpiresAt()).isEqualTo(claims.getExpirationTime().toInstant());
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(
                claims.getIssueTime().toInstant().plusSeconds(response.getExpiresIn()));
    }

    @Test
    void logoutStoresJwtExpiryAsUtcLocalDateTimeAndRevokesRefreshToken() throws Exception {
        Instant expiry = Instant.parse("2099-01-02T03:04:05Z");

        service.logout("Bearer " + signedToken("logout-token", expiry), "refresh-token");

        ArgumentCaptor<InvalidatedToken> token = ArgumentCaptor.forClass(InvalidatedToken.class);
        verify(invalidatedTokenRepository).save(token.capture());
        assertThat(token.getValue().getId()).isEqualTo("logout-token");
        assertThat(token.getValue().getExpiryTime()).isEqualTo(LocalDateTime.of(2099, 1, 2, 3, 4, 5));
        verify(refreshTokenService).revokeRefreshToken("refresh-token");
    }

    @Test
    void logoutDoesNotStoreAlreadyExpiredJwt() throws Exception {
        service.logout("Bearer " + signedToken("expired-token", Instant.parse("2000-01-01T00:00:00Z")), null);

        verifyNoInteractions(invalidatedTokenRepository, refreshTokenService);
    }

    @Test
    void logoutWithoutAccessTokenStillRevokesRefreshToken() {
        service.logout(null, "refresh-token");

        verify(refreshTokenService).revokeRefreshToken("refresh-token");
        verifyNoInteractions(invalidatedTokenRepository);
    }

    @Test
    void logoutRejectsMalformedJwtWithoutSavingBlacklistEntry() {
        AppException exception = assertThrows(AppException.class, () -> service.logout("Bearer malformed", null));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
        verifyNoInteractions(invalidatedTokenRepository, refreshTokenService);
    }

    private String signedToken(String id, Instant expiry) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), new JWTClaimsSet.Builder()
                .issuer("koreanpathway.com")
                .subject(UUID.randomUUID().toString())
                .jwtID(id)
                .expirationTime(Date.from(expiry))
                .build());
        jwt.sign(new MACSigner(TEST_SIGNING_KEY.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
