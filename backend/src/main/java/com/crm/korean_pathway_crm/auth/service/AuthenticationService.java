package com.crm.korean_pathway_crm.auth.service;

import com.crm.korean_pathway_crm.auth.dto.AuthenticationRequest;
import com.crm.korean_pathway_crm.auth.dto.AuthenticationResponse;
import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.auth.dto.IntrospectRequest;
import com.crm.korean_pathway_crm.auth.dto.IntrospectResponse;
import com.crm.korean_pathway_crm.auth.entity.InvalidatedToken;
import com.crm.korean_pathway_crm.auth.entity.RefreshToken;
import com.crm.korean_pathway_crm.auth.repository.InvalidatedTokenRepository;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    JwtProperties jwtProperties;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    RefreshTokenService refreshTokenService;
    InvalidatedTokenRepository invalidatedTokenRepository;
    JwtDecoder jwtDecoder;

    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean valid;

        try {
            jwtDecoder.decode(request.getToken());
            valid = true;
        } catch (JwtException exception) {
            valid = false;
        }

        return IntrospectResponse.builder()
                .valid(valid)
                .build();
    }

    // Sinh Access Token có gắn JTI duy nhất và Subject là User ID (UUID)
    private String generateToken(User user, String jti, Instant issuedAt, Instant expiresAt) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer(jwtProperties.getIssuer())
                .issueTime(Date.from(issuedAt))
                .jwtID(jti)
                .expirationTime(Date.from(expiresAt))
                .claim("scope", buildScope(user.getRole()))
                .claim("roleId", user.getRole().getId().toString())
                .claim("passwordChangedAt", user.getPasswordChangedAt() == null ? "" : user.getPasswordChangedAt().toString())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(jwtProperties.signingKeyBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot generate token: {}", e.getClass().getSimpleName());
            throw new IllegalStateException("Cannot generate token", e);
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
    /**
     * Xử lý Refresh Token: xoay vòng (rotate) token cũ và sinh cặp token mới.
     * @Transactional giúp giữ Hibernate session mở để lazy-load user.getRole()
     * khi gọi generateToken() sau khi rotateAndGetNewToken() hoàn tất.
     */
    @Transactional
    public AuthenticationResponse refreshToken(String refreshTokenStr) {
        RefreshToken newRefreshToken = refreshTokenService.rotateAndGetNewToken(refreshTokenStr);
        return createTokenResponse(newRefreshToken.getUser(), newRefreshToken, Instant.now());
    }
    @Transactional(noRollbackFor = AppException.class)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String username = request.getUsername().trim();
        User user = userRepository.findByUsernameIgnoreCaseForUpdate(username)
                .orElseThrow(() -> new AppException(ErrorCode.AUTHENTICATION_FAILED));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (!user.isActive() || user.getRole() == null || user.getRole().getStatus() != RoleStatus.ACTIVE) {
            throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
        }
        if (user.getLockedUntil() != null) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
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

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);
        return createTokenResponse(user, refreshToken, now.toInstant(ZoneOffset.UTC));
    }

    private AuthenticationResponse createTokenResponse(User user, RefreshToken refreshToken, Instant issuedAt) {
        // JWT NumericDate has second precision; return the same expiry that is signed into the JWT.
        Instant tokenIssuedAt = issuedAt.truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = tokenIssuedAt.plusSeconds(jwtProperties.getExpiration());
        String accessToken = generateToken(user, UUID.randomUUID().toString(), tokenIssuedAt, expiresAt);
        return AuthenticationResponse.builder()
                .authenticated(true)
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtProperties.getExpiration())
                .accessTokenExpiresAt(expiresAt)
                .build();
    }

    @Transactional
    public void logout(String authHeader, String refreshToken) {
        if (authHeader != null && !authHeader.isBlank()) {
            if (!authHeader.startsWith("Bearer ")) {
                throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
            }
            String accessToken = authHeader.substring(7);
            try {
                SignedJWT signedJWT = SignedJWT.parse(accessToken);
                JWSVerifier verifier = new MACVerifier(jwtProperties.signingKeyBytes());
                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                if (!JWSAlgorithm.HS512.equals(signedJWT.getHeader().getAlgorithm())
                        || !signedJWT.verify(verifier)
                        || !jwtProperties.getIssuer().equals(claims.getIssuer())) {
                    throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
                }

                String jti = claims.getJWTID();
                Date expiryTime = claims.getExpirationTime();
                if (jti == null || jti.isBlank() || expiryTime == null) {
                    throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
                }
                if (expiryTime.toInstant().isAfter(Instant.now())) {
                    InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                            .id(jti)
                            .expiryTime(LocalDateTime.ofInstant(expiryTime.toInstant(), ZoneOffset.UTC))
                            .build();
                    invalidatedTokenRepository.save(invalidatedToken);
                }
            } catch (ParseException | JOSEException exception) {
                throw new AppException(ErrorCode.AUTHENTICATION_FAILED);
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
    }
}
