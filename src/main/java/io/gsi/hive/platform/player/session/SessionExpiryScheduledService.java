package io.gsi.hive.platform.player.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@ConditionalOnProperty(name = "hive.session.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class SessionExpiryScheduledService {

    private final SessionRepository sessionRepository;
    private final SessionExpiryConfigProperties sessionExpiryConfigProperties;
    private final SessionConfigProperties sessionConfigProperties;

    public SessionExpiryScheduledService(SessionRepository sessionRepository,
                                         SessionExpiryConfigProperties sessionExpiryConfigProperties,
                                         SessionConfigProperties sessionConfigProperties) {
        this.sessionRepository = sessionRepository;
        this.sessionExpiryConfigProperties = sessionExpiryConfigProperties;
        this.sessionConfigProperties = sessionConfigProperties;
    }

    @Scheduled(initialDelayString = "${hive.session.expiry.initialDelay:30000}", fixedDelayString = "${hive.session.expiry.fixedDelay:30000}")
    @Transactional
    public void expireSessions() {log.info("Looking for inactive sessions to expire");
        if (Boolean.TRUE.equals(sessionExpiryConfigProperties.getBatched())) {
            sessionRepository.batchExpireInactiveSessions(sessionExpiryConfigProperties.getBatchSize(), sessionConfigProperties.getExpirySecs());
        }
        else {
            sessionRepository.expireAllInactiveSessions(sessionConfigProperties.getExpirySecs());
        }
    }
}