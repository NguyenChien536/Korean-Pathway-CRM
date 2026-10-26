package com.crm.korean_pathway_crm.auth.service;

import com.crm.korean_pathway_crm.auth.entity.RefreshToken;
import com.crm.korean_pathway_crm.auth.config.JwtProperties;
import com.crm.korean_pathway_crm.auth.repository.RefreshTokenRepository;
import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshToken generateRefreshToken(User user) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(now.plusSeconds(jwtProperties.getRefreshExpiration()))
                .createdAt(now)
                .isRevoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateAndGetNewToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        RefreshToken currentToken = refreshTokenRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (currentToken.isRevoked() || !currentToken.getExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        User user = currentToken.getUser();
        if (!user.isActive() || user.getRole() == null || user.getRole().getStatus() != RoleStatus.ACTIVE
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Hủy bỏ token hiện tại
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);

        // Sinh token mới thay thế
        return generateRefreshToken(currentToken.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        // Repeated logout is harmless, including an already-removed token.
        refreshTokenRepository.findByTokenForUpdate(token).ifPresent(currentToken -> {
            currentToken.setRevoked(true);
            refreshTokenRepository.save(currentToken);
        });
    }

    @Transactional
    public void revokeAllUserRefreshTokens(String username) {
        refreshTokenRepository.revokeAllByUsername(username);
    }
}
