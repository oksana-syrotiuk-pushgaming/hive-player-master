package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.SessionBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Sql(statements= {PersistenceITBase.CLEAN_DB_SQL}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestPropertySource(properties={"hive.session.expiry.batchSize=2", "hive.session.expiry.batched=true"})
public class SessionExpiryBatchedScheduledServiceIT extends PersistenceITBase {
    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionExpiryScheduledService sessionExpiryScheduledService;

    @Test
    public void givenSessionInactive_whenExpireSessions_thenSessionExpired() {
        Session session = SessionBuilder.aSession()
                .withLastAccessedTime(0L)
                .build();
        sessionRepository.saveAndFlush(session);
        sessionExpiryScheduledService.expireSessions();
        List<Session> sessions = sessionRepository.findAll();
        assertEquals(1, sessions.size());
        assertEquals(SessionStatus.EXPIRED.name(), sessions.get(0).getSessionStatus().name());
    }

    @Test
    public void givenMoreSessionsThanBatchSize_whenExpireSessions_thenBatchSizeSessionsExpired() {
        Session session1 = SessionBuilder.aSession()
                .withId("session_1")
                .withLastAccessedTime(0L)
                .build();
        Session session2 = SessionBuilder.aSession()
                .withId("session_2")
                .withLastAccessedTime(0L)
                .build();
        Session session3 = SessionBuilder.aSession()
                .withId("session_3")
                .withLastAccessedTime(0L)
                .build();
        sessionRepository.saveAndFlush(session1);
        sessionRepository.saveAndFlush(session2);
        sessionRepository.saveAndFlush(session3);

        sessionExpiryScheduledService.expireSessions();
        List<Session> activeSessions = sessionRepository.findAll().stream().filter(session -> session.getSessionStatus().equals(SessionStatus.ACTIVE)).collect(Collectors.toList());
        List<Session> inactiveSessions = sessionRepository.findAll().stream().filter(session -> session.getSessionStatus().equals(SessionStatus.EXPIRED)).collect(Collectors.toList());
        assertEquals(2, inactiveSessions.size());
        assertEquals(SessionStatus.EXPIRED.name(), inactiveSessions.get(0).getSessionStatus().name());
        assertEquals(1, activeSessions.size());
        assertEquals(SessionStatus.ACTIVE.name(), activeSessions.get(0).getSessionStatus().name());
    }

    @Test
    public void givenNewSessionAddedAfterFirstBatch_whenExpireSessions_thenBatchSizeSessionsExpired() {
        Session session1 = SessionBuilder.aSession()
                .withId("session_1")
                .withLastAccessedTime(0L)
                .build();
        Session session2 = SessionBuilder.aSession()
                .withId("session_2")
                .withLastAccessedTime(0L)
                .build();
        Session session3 = SessionBuilder.aSession()
                .withId("session_3")
                .withLastAccessedTime(0L)
                .build();
        Session session4 = SessionBuilder.aSession()
                .withId("session_4")
                .withLastAccessedTime(0L)
                .build();
        Session session5 = SessionBuilder.aSession()
                .withId("session_5")
                .withLastAccessedTime(0L)
                .build();
        sessionRepository.saveAndFlush(session1);
        sessionRepository.saveAndFlush(session2);
        sessionRepository.saveAndFlush(session3);

        sessionExpiryScheduledService.expireSessions();

        List<Session> activeSessions = sessionRepository.findAll().stream().filter(session -> session.getSessionStatus().equals(SessionStatus.ACTIVE)).collect(Collectors.toList());
        List<Session> inactiveSessions = sessionRepository.findAll().stream().filter(session -> session.getSessionStatus().equals(SessionStatus.EXPIRED)).collect(Collectors.toList());
        assertEquals("Batch size is 2 so 1 active session left", 1, activeSessions.size());
        assertEquals(2, inactiveSessions.size());

        sessionRepository.saveAndFlush(session4);
        sessionRepository.saveAndFlush(session5);

        sessionExpiryScheduledService.expireSessions();

        List<Session> allSessions = sessionRepository.findAll();
        List<Session> oldestActiveSession = allSessions.stream().filter(session -> session.getId().equals("session_3")).collect(Collectors.toList());
        List<Session> mostRecentSession = allSessions.stream().filter(session -> session.getId().equals("session_5")).collect(Collectors.toList());

        assertEquals(1, oldestActiveSession.size());
        assertEquals("Check to ensure session 3 is in next batch.", SessionStatus.EXPIRED.name(), oldestActiveSession.get(0).getSessionStatus().name());
        assertEquals(1, mostRecentSession.size());
        assertEquals("Batch size is 2 so 1 active session left", SessionStatus.ACTIVE.name(), mostRecentSession.get(0).getSessionStatus().name());
    }
}
