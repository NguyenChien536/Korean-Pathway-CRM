package com.crm.korean_pathway_crm.auth.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    boolean authenticated;
    String tokenType;
    String accessToken;
    String refreshToken;
    long expiresIn;
    Instant accessTokenExpiresAt;
}
