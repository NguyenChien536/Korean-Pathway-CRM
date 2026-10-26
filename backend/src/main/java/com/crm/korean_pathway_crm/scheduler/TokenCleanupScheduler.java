package com.crm.korean_pathway_crm.scheduler;

import com.crm.korean_pathway_crm.auth.repository.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Bangkok")
    @Transactional
    public void cleanExpiredInvalidatedTokens() {
        int removed = invalidatedTokenRepository.deleteExpiredTokens(LocalDateTime.now(ZoneOffset.UTC));
        log.info("Removed {} expired blacklist entries", removed);
    }
}
