package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.SessionBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.Assert.assertEquals;

@Sql(statements= {PersistenceITBase.CLEAN_DB_SQL}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SessionExpiryScheduledServiceIT extends PersistenceITBase {
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
    public void givenSessionActive_whenExpireSessions_thenSessionNotUpdated() {
        Session session = SessionBuilder.aSession()
                .build();
        sessionRepository.saveAndFlush(session);
        sessionExpiryScheduledService.expireSessions();
        List<Session> sessions = sessionRepository.findAll();
        assertEquals(1, sessions.size());
        assertEquals(SessionStatus.ACTIVE.name(), sessions.get(0).getSessionStatus().name());
    }

    @Test
    public void givenActiveAndInactiveSession_whenExpireSessions_thenInactiveSessionExpired() {
        Session activeSession = SessionBuilder.aSession()
                .withId("activeId")
                .build();
        Session inactiveSession = SessionBuilder.aSession()
                .withLastAccessedTime(0L)
                .withSessionStatus(SessionStatus.EXPIRED)
                .withId("inactiveId")
                .build();
        sessionRepository.saveAndFlush(activeSession);
        sessionRepository.saveAndFlush(inactiveSession);
        sessionExpiryScheduledService.expireSessions();
        List<Session> sessions = sessionRepository.findAll();
        assertEquals(2, sessions.size());
        assertEquals(SessionStatus.ACTIVE.name(), sessions.get(0).getSessionStatus().name());
        assertEquals(SessionStatus.EXPIRED.name(), sessions.get(1).getSessionStatus().name());
    }

    @Test
    public void givenExpiredSession_whenExpireSessions_thenSessionNotUpdated() {
        Session inactiveSession = SessionBuilder.aSession()
                .withLastAccessedTime(0L)
                .withSessionStatus(SessionStatus.EXPIRED)
                .withId("inactiveId")
                .build();
        sessionRepository.saveAndFlush(inactiveSession);
        sessionExpiryScheduledService.expireSessions();
        List<Session> sessions = sessionRepository.findAll();
        assertEquals(1, sessions.size());
        assertEquals(SessionStatus.EXPIRED.name(), sessions.get(0).getSessionStatus().name());
    }

    @Test
    public void givenNoSessions_whenExpireSessions_thenNoUpdates() {
        sessionExpiryScheduledService.expireSessions();
        List<Session> sessions = sessionRepository.findAll();
        assertEquals(0, sessions.size());
    }

    @Test
    public void givenFinishedSession_whenExpireSessions_thenSessionNotUpdated() {
        Session finishedSession = SessionBuilder.aSession()
                .withLastAccessedTime(0L)
                .withSessionStatus(SessionStatus.FINISHED)
                .withId("finishedId")
                .build();
        sessionRepository.saveAndFlush(finishedSession);
        sessionExpiryScheduledService.expireSessions();
        List<Session> sessions = sessionRepository.findAll();
        assertEquals(1, sessions.size());
        assertEquals(SessionStatus.FINISHED.name(), sessions.get(0).getSessionStatus().name());
    }
}