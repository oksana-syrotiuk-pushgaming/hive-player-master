/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={HivePlayer.class},webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestExecutionListeners({MockitoTestExecutionListener.class, ResetMocksTestExecutionListener.class })
@TestPropertySource("/config/test.properties")
public abstract class PersistenceITBase extends AbstractTransactionalJUnit4SpringContextTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final String TXN_REQUEST_EVENT_JSON = "[{\"type\":\"txnRequest\",\"timestamp\":1518091833895,\"txnId\":\"1000-1\",\"gameCode\":\"testGame\","
			+ "\"playId\":\"1000-10\",\"playComplete\":true,\"playCompleteIfCancelled\":true,\"roundId\":\"1000-10\",\"roundComplete\":true,\"roundCompleteIfCancelled\":true,\""
			+ "playerId\":\"player1\",\"guest\":false,\"igpCode\":\"iguana\",\"mode\":\"real\",\"ccyCode\":\"GBP\",\"txnType\":\"WIN\",\"amount\":1.00,\"jackpotAmount\":10,\"sessionId\":\"testSession\"}]";

	public static final String STAKE_TXN_SQL =
			"insert into t_txn values ('1000-1','testGame', '1000-10','t','t','1000-10','t','t', 'player1','iguana','testToken'," +
					"'session', 'real','f','GBP','STAKE','20.00','10.00','1970-01-01T00:00Z',null," +
					"null,null,'PENDING',null,null,0,'" + TXN_REQUEST_EVENT_JSON + "', null);";

	public static final String OPFRSTK_TXN_SQL =
			"insert into t_txn values ('1000-1', 'testGame', '1000-10','t','t','1000-10','t','t', 'player1','iguana','testToken'," +
					"'session', 'real','f','GBP','OPFRSTK','20.00','10.00','1970-01-01T00:00Z',null," +
					"null,null,'PENDING',null,null,0,'" + TXN_REQUEST_EVENT_JSON + "', null);";


	public static final String PLAYER_SQL =
			"insert into t_player (player_id,igp_code,guest,ccy_code,username,alias,country, lang)" +
					" values ('player1','iguana','f','GBP','test1','test1','GB', 'en');";

	public static final String GUEST_PLAYER_SQL =
			"insert into t_player (player_id,igp_code,guest,ccy_code,username,alias,country, lang)" +
					" values ('player1','iguana','t','GBP','test1','test1','GB', 'en');";


	public static final String AUTOCOMPLETE_REQUEST_SQL = "insert into t_autocomplete_request_q (play_id,game_code, guest ,created_at, retries, session_id) VALUES ('1001-10','testGame', false, '1970-01-01T00:00Z', 0, 'testSession')";

	public static final String CLEAN_DB_SQL = "truncate t_session; truncate t_txn_audit cascade; truncate t_txn cascade; truncate t_player cascade; truncate t_txn_callback_q cascade; truncate t_autocomplete_request_q cascade; truncate t_bigwin cascade; truncate t_txn_cleardown cascade";
}
