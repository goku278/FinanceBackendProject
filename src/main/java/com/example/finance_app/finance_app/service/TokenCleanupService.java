package com.example.finance_app.finance_app.service;

import com.example.finance_app.finance_app.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class TokenCleanupService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Scheduled(cron = "0 0 * * * *") // run every hour
    @Transactional
    public void cleanExpiredTokens() {
        Instant now = Instant.now();
        int deleted = blacklistedTokenRepository.deleteAllByExpiryDateBefore(now);
        if (deleted > 0) {
            log.info("Removed {} expired blacklisted tokens", deleted);
        }
    }
}