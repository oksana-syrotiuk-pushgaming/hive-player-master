package io.gsi.hive.platform.player.registry.gameInfo;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.registry.RegistryGateway;
import io.gsi.hive.platform.player.registry.gameInfo.validation.ValidationLevel;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class GameIdService {
    private final RegistryGateway registryGateway;
    private final ValidationLevel validationLevel;

    public GameIdService(
            RegistryGateway registryGateway,
            @Value("${hive.game.id.validation.level:PERMISSIVE}") ValidationLevel validationLevel
    ) {
        this.registryGateway = registryGateway;
        this.validationLevel = validationLevel;
    }

    public void validateGameId(TxnRequest txnRequest, Session session) {
        if (!validationLevel.equals(ValidationLevel.NONE)) {
            try {
                ValidGameIds validGameIds = registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class);
                Map<String, Integer> gameIds = validGameIds.getGameCodeToGameId();
                if (validGameIds.getGameCodeToGameId() == null || !validGameIds.getGameCodeToGameId().containsKey(session.getGameCode())) {
                    BadRequestException gameIdNotFoundBadRequestException = new BadRequestException(
                            String.format("gameId not found in platform config for gameCode: %s", session.getGameCode()));
                    handleException(txnRequest, gameIdNotFoundBadRequestException);
                    return;
                }
                int sessionGameId = gameIds.get(session.getGameCode());
                int txnGameId = extractGameId(txnRequest);
                if (txnGameId != sessionGameId) {
                    BadRequestException gameIdMismatchBadRequestException = new BadRequestException(
                            String.format("txn gameId does not match session gameId, playId: %s, txnId: %s", txnRequest.getTxnId(), txnRequest.getPlayId()));
                    handleException(txnRequest, gameIdMismatchBadRequestException);
                }
            } catch (NotFoundException | InternalServerException exception) {
                handleException(txnRequest, exception);
            }
        }
    }

    private void handleException(TxnRequest txnRequest, RuntimeException exception) {
        publishCounter(txnRequest);
        log.warn(exception.toString());
        if (validationLevel.equals(ValidationLevel.STRICT)) {
            throw exception;
        }
    }

    private Integer extractGameId(TxnRequest txnRequest) {
        return Integer.parseInt(txnRequest.getTxnId().split("-")[0]);
    }

    private void publishCounter(TxnRequest txnRequest) {
        Tags tags = Tags.of("error", "gameIdValidationError", "gameCode", txnRequest.getGameCode());
        Counter counter = Counter.builder("validation#gameId").tags(tags).register(Metrics.globalRegistry);
        counter.increment();
    }
}
