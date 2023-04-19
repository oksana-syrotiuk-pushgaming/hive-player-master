package io.gsi.hive.platform.player.registry.gameInfo;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.registry.RegistryGateway;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.txn.TxnRequestPresets;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
public class GameIdServicePermissiveValidationIT {
    @MockBean
    private RegistryGateway registryGateway;

    @Autowired
    private GameIdService gameIdService;

    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setup() {
        Metrics.globalRegistry.forEachMeter(Metrics.globalRegistry::remove);
        when(registryGateway.getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        )).thenReturn(ValidGameIds.builder().gameCodeToGameId(Map.of("testGame", 1000, "testGame2", 2000)).build());
        Logger logger = (Logger) LoggerFactory.getLogger(GameIdService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    public void givenStakeTxnGameIdMatchesSessionGameId_whenValidateGameId_thenNoExceptionThrown() {
        TxnRequest stakeTxnRequest = TxnRequestPresets.defaultStakeTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame");
        gameIdService.validateGameId(stakeTxnRequest, session);
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
    }

    @Test
    public void givenWinTxnGameIdMatchesSessionGameId_whenValidateGameId_thenNoExceptionThrown() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame");
        gameIdService.validateGameId(winTxnRequest, session);
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
    }

    @Test
    public void givenStakeTxnGameIdDoesNotMatchSessionGameId_whenValidateGameId_thenNoExceptionThrown() {
        TxnRequest stakeTxnRequest = TxnRequestPresets.defaultStakeTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        gameIdService.validateGameId(stakeTxnRequest, session);
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
    }

    @Test
    public void givenWinTxnGameIdDoesNotMatchSessionGameId_whenValidateGameId_thenNoExceptionThrownAndCounterIncrementedAndIssueLogged() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        gameIdService.validateGameId(winTxnRequest, session);
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
        Counter gameIdValidationCounter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(gameIdValidationCounter.count()).isEqualTo(1);
        assertThat(listAppender.list).extracting("message")
                .contains("io.gsi.commons.exception.BadRequestException: txn gameId does not match session gameId, playId: 1000-1, txnId: 1000-10");
    }

    @Test
    public void givenSessionGameIdNotFoundInPlatformConfig_whenValidateGameId_thenNoExceptionThrownAndCounterIncrementedAndIssueLogged() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame3");
        gameIdService.validateGameId(winTxnRequest, session);
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
        Counter gameIdValidationCounter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(gameIdValidationCounter.count()).isEqualTo(1);
        assertThat(listAppender.list).extracting("message")
                .contains("io.gsi.commons.exception.BadRequestException: gameId not found in platform config for gameCode: testGame3");
    }

    @Test
    public void givenNullGameIdsInPlatformConfig_whenValidateGameId_thenNoExceptionThrownAndCounterIncrementedAndIssueLogged() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame");
        when(registryGateway.getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        )).thenReturn(ValidGameIds.builder().gameCodeToGameId(null).build());
        gameIdService.validateGameId(winTxnRequest, session);
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter.count()).isEqualTo(1);
        assertThat(listAppender.list).extracting("message")
                .contains("io.gsi.commons.exception.BadRequestException: gameId not found in platform config for gameCode: testGame");
    }

    @Test
    public void givenGetConfigThrowsNotFoundException_whenValidateGameId_thenNoExceptionThrownAndCounterIncrementedAndIssueLogged() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode(GamePresets.CODE);
        when(registryGateway.getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        )).thenThrow(new NotFoundException("No valid configuration found"));
        gameIdService.validateGameId(winTxnRequest, session);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter.count()).isEqualTo(1);
        assertThat(listAppender.list).extracting("message")
                .contains("io.gsi.commons.exception.NotFoundException: No valid configuration found");
    }
}
