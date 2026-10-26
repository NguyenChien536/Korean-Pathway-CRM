package com.crm.korean_pathway_crm.auth.service;

import com.crm.korean_pathway_crm.auth.entity.RefreshToken;
import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.auth.repository.RefreshTokenRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenDateTimeTest {
    @Mock
    private RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, new JwtProperties());
    }

    @Test
    void refreshTokenUsesUtcCreationTimeAndSevenDayLifetime() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant beforeCreation = Instant.now();

        RefreshToken token = service.generateRefreshToken(User.builder().build());

        Instant afterCreation = Instant.now();
        assertThat(token.getCreatedAt().toInstant(ZoneOffset.UTC)).isBetween(beforeCreation, afterCreation);
        assertThat(token.getExpiresAt()).isEqualTo(token.getCreatedAt().plusDays(7));
    }
}
