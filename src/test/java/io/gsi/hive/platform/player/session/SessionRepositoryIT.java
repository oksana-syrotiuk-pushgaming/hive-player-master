package io.gsi.hive.platform.player.session;

import static io.gsi.commons.test.string.StringUtils.generateRandomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.SessionBuilder;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(statements= {PersistenceITBase.CLEAN_DB_SQL}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SessionRepositoryIT extends PersistenceITBase {

	@Autowired
	private SessionRepository sessionRepository;

	@Test
	public void nullWhenNotFound() {
		assertThat(sessionRepository.findById("abcd").isEmpty(), is(true));
	}

	@Test
	public void givenNonExistentGameplaySessionToken_whenFindGameplaySessionByToken_thenReturnNull() {
		assertNull(sessionRepository.findBySessionToken("nonExistentSessionToken"));
	}

	@Test
	public void saveAndFind() {
		Session session = exampleSession();
		sessionRepository.save(session);
		Session sessionRetrieved = sessionRepository.findById(session.getId()).get();
		assertThat(sessionRetrieved, equalTo(session));
	}

	@Test
	public void saveAndFindWithLongAuthToken() {
		Session session = longAuthTokenSession();
		sessionRepository.save(session);
		Session sessionRetrieved = sessionRepository.findById(session.getId()).get();
		assertThat(sessionRetrieved.getAccessToken(), equalTo(session.getAccessToken()));
	}

	@Test
	public void deleteByLastAccessedTimeLessThan() {
		Session session = exampleSession();
		session.setLastAccessedTime(1234L);
		sessionRepository.save(session);
		sessionRepository.deleteByLastAccessedTimeLessThan(12345L);
		assertThat(sessionRepository.findById(session.getId()).isEmpty(), is(true));
	}

	@Test
	public void deleteByLastAccessedTimeLessThanNoMatches() {
		Session session = exampleSession();
		session.setLastAccessedTime(99999L);
		sessionRepository.save(session);
		sessionRepository.deleteByLastAccessedTimeLessThan(12345L);
		Session sessionRetrieved = sessionRepository.findById(session.getId()).get();
		assertThat(sessionRetrieved, equalTo(session));
	}

	@Test
	public void givenValidActiveSession_whenFindByStatusAndPlayerId_thenReturnActiveSession() {
		Session session = exampleSession();
		session.setSessionStatus(SessionStatus.ACTIVE);
		sessionRepository.save(session);
		List<Session> activeSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(session.getSessionStatus(), session.getPlayerId(), session.getIgpCode());

		Assertions.assertThat(activeSessions).contains(session);
	}

	@Test
	public void okSaveSessionMaxLang() {
		Session session = SessionBuilder.aSession()
				.withLang("aaaa")
				.build();
		session = sessionRepository.saveAndFlush(session);
		assertThat(session.getLang(), equalTo("aaaa"));
	}

	private Session exampleSession() {
		return SessionBuilder.aSession().build();
	}

	private Session longAuthTokenSession() {
		return SessionBuilder.aSession().withAccessToken(generateRandomString(256)).build();
	}

}
