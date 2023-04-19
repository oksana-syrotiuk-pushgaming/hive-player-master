package io.gsi.hive.platform.player.registry.gameInfo;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.registry.RegistryGateway;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.txn.TxnRequestPresets;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(value = "/config/test.properties", properties = {
        "hive.game.id.validation.level=NONE"
})
public class GameIdServiceNoValidationIT {
    @MockBean
    private RegistryGateway registryGateway;

    @Autowired
    private GameIdService gameIdService;

    @Before
    public void setup() {
        Metrics.globalRegistry.forEachMeter(Metrics.globalRegistry::remove);
    }

    @After
    public void verifyRegistryConfigCalledZeroTimes() {
        verify(registryGateway, times(0)).getConfig(
                any(), any(), any(), any()
        );
    }

    @Test
    public void givenStakeTxnGameIdMatchesSessionGameId_whenValidateGameId_thenNoExceptionThrownAndCounterIsNull() {
        TxnRequest stakeTxnRequest = TxnRequestPresets.defaultStakeTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame");
        gameIdService.validateGameId(stakeTxnRequest, session);

        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenWinTxnGameIdMatchesSessionGameId_whenValidateGameId_thenNoExceptionThrownAndCounterIsNull() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame");
        gameIdService.validateGameId(winTxnRequest, session);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenStakeTxnGameIdDoesNotMatchSessionGameId_whenValidateGameId_thenNoExceptionThrownAndCounterIsNull() {
        TxnRequest stakeTxnRequest = TxnRequestPresets.defaultStakeTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        gameIdService.validateGameId(stakeTxnRequest, session);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenWinTxnGameIdDoesNotMatchSessionGameId_whenValidateGameId_thenNoExceptionThrownAndCounterIsNull() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        gameIdService.validateGameId(winTxnRequest, session);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenSessionGameIdNotFoundInPlatformConfig_whenValidateGameId_thenNoExceptionThrownAndCounterIsNull() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame3");
        gameIdService.validateGameId(winTxnRequest, session);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenNullGameIdsInPlatformConfig_whenValidateGameId_thenNoExceptionThrownAndCounterIsNull() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame");
        gameIdService.validateGameId(winTxnRequest, session);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenGetConfigThrowsNotFoundException_whenValidateGameId_thenNoExceptionThrownAndCounterIsNull() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode(GamePresets.CODE);
        gameIdService.validateGameId(winTxnRequest, session);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }
}
