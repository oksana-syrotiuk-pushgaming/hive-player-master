package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.GameplaySessionBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.Assert.assertFalse;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL}, executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestPropertySource(properties={"hive.session.token.single-use=false"})
public class SessionServiceSingleUseTokenIT extends ApiITBase {

    @Autowired
    private SessionService sessionService;

    @Test
    public void givenMultiUseToken_whenSingleUseTokenInvalidCheck_thenReturnFalse() {
        GameplaySession session = GameplaySessionBuilder.aSession().build();
        assertFalse(sessionService.isSingleUseTokenInvalid(session));
    }

    @Test
    public void givenMultiUseTokenUsed_whenSingleUseTokenInvalidCheck_thenReturnFalse() {
        GameplaySession session = GameplaySessionBuilder.aSession().build();
        session.setTokenUsed(true);
        assertFalse(sessionService.isSingleUseTokenInvalid(session));
    }
}
