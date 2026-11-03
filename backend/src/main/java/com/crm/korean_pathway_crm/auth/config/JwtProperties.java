package com.crm.korean_pathway_crm.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {
    @NotBlank(message = "Set JWT_SIGNER_KEY or run backend/scripts/initialize-local-jwt.ps1")
    private String signerKey;

    @NotBlank
    private String issuer = "koreanpathway.com";

    // Lifetimes are measured in seconds.
    @Min(1)
    private long expiration = 3600;

    @Min(1)
    private long refreshExpiration = 604800;

    @AssertTrue(message = "JWT_SIGNER_KEY must contain at least 64 UTF-8 bytes for HS512; run backend/scripts/initialize-local-jwt.ps1 to create a local key")
    public boolean isSigningKeyValid() {
        return signerKey == null || signerKey.isBlank()
                || signerKey.getBytes(StandardCharsets.UTF_8).length >= 64;
    }

    public byte[] signingKeyBytes() {
        if (signerKey == null || signerKey.isBlank() || !isSigningKeyValid()) {
            throw new IllegalStateException("JWT_SIGNER_KEY must contain at least 64 UTF-8 bytes for HS512");
        }
        return signerKey.getBytes(StandardCharsets.UTF_8);
    }
}
