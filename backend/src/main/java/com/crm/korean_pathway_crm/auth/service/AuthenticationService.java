package com.crm.korean_pathway_crm.auth.service;

import com.crm.korean_pathway_crm.auth.repository.InvalidatedTokenRepository;
import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.user.entity.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;

@Slf4j
@EnableConfigurationProperties(JwtProperties.class)
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    JwtProperties jwtProperties;
    RefreshTokenService refreshTokenService;
    InvalidatedTokenRepository invalidatedTokenRepository;


    // Sinh Access Token có gắn JTI duy nhất và Subject là User ID (UUID)
    private String generateToken(User user, String jti) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString()) // Lưu ID người dùng (chuỗi bất biến)
                .issuer("koreanpathway.com")
                .issueTime(new Date())
                .jwtID(jti) // Gắn JTI làm định danh duy nhất cho Access Token
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
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
    private String buildScope(Role role){
        StringJoiner joiner = new StringJoiner(" ");

        if (role == null || role.getStatus() == RoleStatus.INACTIVE) {
            return "";
        }
        joiner.add("ROLE_" + role.getCode());
        return joiner.toString();
    }
}
