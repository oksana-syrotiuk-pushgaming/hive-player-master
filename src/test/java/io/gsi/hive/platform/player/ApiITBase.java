package io.gsi.hive.platform.player;

import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.PlayService;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerRepository;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.Txn;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
@SpringBootTest(classes = {HivePlayer.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiITBase {

    @LocalServerPort
    protected int serverPort;

    @Before
    public void setupRestAssured() {
        RestAssured.port = serverPort;
    }

    protected ZonedDateTime defaultZonedDateTime;
    protected String defaultDateFrom;
    protected String defaultDateTo;

    @Value("${hive.player.apiKey}")
    protected String hivePlayerApiKeyValue;

    @Autowired
    protected TxnRepository txnRepository;

    @Autowired
    protected GameService gameService;

    @Autowired
    protected PlayerRepository playerRepository;

    @Autowired
    protected PlayService playService;

    @SpyBean
    @Autowired @Qualifier("reportNamedParameterJdbcTemplateLongerDefaultTimeout")
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    protected void saveDefaultPlayer() {
        playerRepository.save(PlayerBuilder.aPlayer().build());
    }

    protected void savePlayer(Player... players) {
        playerRepository.saveAll(Arrays.asList(players));
    }

    protected void saveDefaultTxn() {
        txnRepository.save(TxnBuilder.txn().build());
    }

    protected void saveTxn(Txn... txn) {
        txnRepository.saveAll(Arrays.asList(txn));
    }

    protected void savePlayersAndTxns() {
        this.savePlayersAndTxns(100);
    }

    protected void savePlayersAndTxns(int numRecords) {
        int startId = 0;
        int maxRecords = numRecords;

        for (int i = startId; i < maxRecords; i++) {
            playerRepository.save(PlayerBuilder
                    .aPlayer()
                    .withPlayerId("player" + i)
                    .withUsername("player" + i)
                    .build());
        }

        for (int i = startId; i < maxRecords; i++) {
            txnRepository.save(TxnBuilder.txn()
                    .withMode(Mode.real)
                    .withTxnId(String.valueOf(i))
                    .withPlayerId("player" + i)
                    .withTxnTs(defaultZonedDateTime.plusMinutes(i))
                    .build());
        }
    }

}
