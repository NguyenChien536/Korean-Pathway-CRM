package com.crm.korean_pathway_crm.auth.service;

import com.crm.korean_pathway_crm.auth.entity.RefreshToken;
import com.crm.korean_pathway_crm.auth.repository.RefreshTokenRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.crm.korean_pathway_crm.common.exception.AppException;
import com.crm.korean_pathway_crm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshToken generateRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7)) // 7 ngày
                .createdAt(LocalDateTime.now())
                .isRevoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateAndGetNewToken(String token) {
        RefreshToken currentToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (currentToken.isRevoked() || currentToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Cơ chế bảo mật Rotation: Nếu token bị phát hiện đã thu hồi hoặc hết hạn, ta ném ngoại lệ yêu cầu login lại
            currentToken.setRevoked(true);
            refreshTokenRepository.save(currentToken);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Hủy bỏ token hiện tại
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);

        // Sinh token mới thay thế
        return generateRefreshToken(currentToken.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken currentToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);
    }

    @Transactional
    public void revokeAllUserRefreshTokens(String username) {
        refreshTokenRepository.revokeAllByUsername(username);
    }
}
