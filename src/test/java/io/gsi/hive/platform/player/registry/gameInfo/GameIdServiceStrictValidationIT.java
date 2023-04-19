package io.gsi.hive.platform.player.registry.gameInfo;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.registry.RegistryGateway;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.txn.TxnRequestPresets;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(value = "/config/test.properties", properties = {"hive.game.id.validation.level=STRICT"})
public class GameIdServiceStrictValidationIT {
    @MockBean
    private RegistryGateway registryGateway;

    @Autowired
    private GameIdService gameIdService;

    @Before
    public void setup() {
        Metrics.globalRegistry.forEachMeter(Metrics.globalRegistry::remove);

        when(registryGateway.getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        )).thenReturn(ValidGameIds.builder().gameCodeToGameId(Map.of("testGame", 1000, "testGame2", 2000)).build());
    }

    @Test
    public void givenStakeTxnGameIdMatchesSessionGameId_whenValidateGameId_thenNullPointerExceptionThrownAndCounterIsNull() {
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
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenWinTxnGameIdMatchesSessionGameId_whenValidateGameId_thenNullPointerExceptionThrownAndCounterIsNull() {
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
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter).isNull();
    }

    @Test
    public void givenStakeTxnGameIdDoesNotMatchSessionGameId_whenValidateGameId_thenBadRequestExceptionThrownAndCorrectCounterIsIncremented() {
        TxnRequest stakeTxnRequest = TxnRequestPresets.defaultStakeTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        assertThatThrownBy(() -> gameIdService.validateGameId(stakeTxnRequest, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("txn gameId does not match session gameId, playId: 1000-1, txnId: 1000-10");
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    public void givenWinTxnGameIdDoesNotMatchSessionGameId_whenValidateGameId_thenBadRequestExceptionThrownAndCorrectCounterIsIncremented() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        assertThatThrownBy(() -> gameIdService.validateGameId(winTxnRequest, session))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("txn gameId does not match session gameId, playId: 1000-1, txnId: 1000-10");

        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );

        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    public void givenSessionGameIdNotFoundInPlatformConfig_whenValidateGameId_thenBadRequestExceptionThrownAndCorrectCounterIsIncremented() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame3");
        assertThatThrownBy(() -> gameIdService.validateGameId(winTxnRequest, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("gameId not found in platform config for gameCode: testGame3");
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    public void givenNullGameIdsInPlatformConfig_whenValidateGameId_thenBadRequestExceptionThrownAndCorrectCounterIsIncremented() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame");
        when(registryGateway.getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        )).thenReturn(ValidGameIds.builder().gameCodeToGameId(null).build());
        assertThatThrownBy(() -> gameIdService.validateGameId(winTxnRequest, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("gameId not found in platform config for gameCode: testGame");
        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    public void givenGetConfigThrowsNotFoundException_whenValidateGameId_thenNotFoundExceptionThrownAndCorrectCounterIsIncremented() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode(GamePresets.CODE);
        when(registryGateway.getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        )).thenThrow(new NotFoundException("No valid configuration found"));
        assertThatThrownBy(() -> gameIdService.validateGameId(winTxnRequest, session))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No valid configuration found");
        Counter counter = Metrics.globalRegistry.find("validation#gameId").counter();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    public void givenWinTxnGameIdDoesNotMatchSessionGameId_whenValidateGameId_thenBadRequestExceptionThrownAndCorrectTagsUsedInCounter() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        assertThatThrownBy(() -> gameIdService.validateGameId(winTxnRequest, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("txn gameId does not match session gameId, playId: 1000-1, txnId: 1000-10");

        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );

        Tags tags = Tags.of("error", "gameIdValidationError", "gameCode", GamePresets.CODE);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").tags(tags).counter();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    public void givenExistingCounterWithSameNameAndTags_whenValidateGameId_thenCounterIncremented() {
        TxnRequest winTxnRequest = TxnRequestPresets.defaultWinTxnRequestBuilder().build();
        Session session = new Session();
        session.setGameCode("testGame2");
        assertThatThrownBy(() -> gameIdService.validateGameId(winTxnRequest, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("txn gameId does not match session gameId, playId: 1000-1, txnId: 1000-10");

        verify(registryGateway).getConfig(
                "GAME_ID",
                "configType",
                "gameIdConfig",
                ValidGameIds.class
        );

        Tags tags = Tags.of("error", "gameIdValidationError", "gameCode", GamePresets.CODE);
        Counter counter = Metrics.globalRegistry.find("validation#gameId").tags(tags).counter();
        assertThat(counter.count()).isEqualTo(1);
        TxnRequest winTxnRequest2 = TxnRequestPresets.defaultWinTxnRequestBuilder().playerId("player2").build();
        Session session2 = new Session();
        session2.setGameCode("testGame2");
        assertThatThrownBy(() -> gameIdService.validateGameId(winTxnRequest2, session2))
                .isInstanceOf(BadRequestException.class);
        assertThat(counter.count()).isEqualTo(2);
    }
}
