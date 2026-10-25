package com.crm.korean_pathway_crm.auth.service;

import com.crm.korean_pathway_crm.auth.dto.AuthenticationRequest;
import com.crm.korean_pathway_crm.auth.dto.AuthenticationResponse;
import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.auth.entity.RefreshToken;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long ACCOUNT_LOCK_MINUTES = 15;
    private static final long ACCESS_TOKEN_LIFETIME_MINUTES = 60;
    private static final String JWT_ISSUER = "koreanpathway.com";

    JwtProperties jwtProperties;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    RefreshTokenService refreshTokenService;


    private String generateToken(User user, String jti, Instant expiresAt) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer(JWT_ISSUER)
                .issueTime(new Date())
                .jwtID(jti)
                .expirationTime(Date.from(expiresAt))
                .claim("scope", buildScope(user.getRole()))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(jwtProperties.getSignerKey().getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot generate token", e);
            throw new RuntimeException(e);
        }
    }
    private String buildScope(Role role) {
        StringJoiner joiner = new StringJoiner(" ");

        if (role == null || role.getStatus() != RoleStatus.ACTIVE) {
            return "";
        }
        joiner.add("ROLE_" + role.getCode());
        return joiner.toString();
    }

    @Transactional(noRollbackFor = AppException.class)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String username = request.getUsername().trim();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new AppException(ErrorCode.AUTHENTICATION_FAILED));

        OffsetDateTime now = OffsetDateTime.now();
        if (!user.isActive() || user.getRole() == null || user.getRole().getStatus() != RoleStatus.ACTIVE) {
            throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failedAttempts);
            if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setLockedUntil(now.plusMinutes(ACCOUNT_LOCK_MINUTES));
            }
            userRepository.saveAndFlush(user);
            throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        Instant accessTokenExpiresAt = now.toInstant().plus(ACCESS_TOKEN_LIFETIME_MINUTES, ChronoUnit.MINUTES);
        String accessToken = generateToken(user, UUID.randomUUID().toString(), accessTokenExpiresAt);
        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .build();
    }
}
