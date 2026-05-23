package com.att.tdp.issueflow.scheduling;

import com.att.tdp.issueflow.security.TokenDenyListRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final TokenDenyListRepository denyListRepository;

    public TokenCleanupScheduler(TokenDenyListRepository denyListRepository) {
        this.denyListRepository = denyListRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int removed = denyListRepository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Token deny-list purge removed {} expired entry(ies)", removed);
        }
    }
}
