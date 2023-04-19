/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.api.s2s;


import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.platformidentifier.PlatformIdentifierService;
import io.gsi.hive.platform.player.recon.ManualReconService;
import io.gsi.hive.platform.player.registry.gameInfo.GameIdService;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.TxnService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/s2s/platform/player/v1")
@Loggable
public class S2STxnController {
	private final TxnService txnService;
	private final SessionService sessionService;
	private final GameService gameService;
	private final PlatformIdentifierService platformIdentifierService;
	private final GameIdService gameIdService;
	private final ManualReconService manualReconService;

	public S2STxnController(
			TxnService txnService,
			SessionService sessionService,
			GameService gameService,
			PlatformIdentifierService platformIdentifierService,
			GameIdService gameIdService,
			ManualReconService manualReconService
	) {
		this.txnService = txnService;
		this.sessionService = sessionService;
		this.gameService = gameService;
		this.platformIdentifierService = platformIdentifierService;
		this.gameIdService = gameIdService;
		this.manualReconService = manualReconService;
	}

	@PostMapping(path="/txn")
	public TxnReceipt processTxn(@RequestBody @Valid TxnRequest gameTxn) {
		if (gameTxn.getTxnType()==TxnType.STAKE) {
			Session session = sessionService.getSession(gameTxn.getSessionId());
			gameIdService.validateGameId(gameTxn, session);
			//TODO consider separating not found exception from expired exception
			if (sessionService.isExpired(session)) {
				//TODO replace with a new SessionExpiryException to distinguish from iGP auth failure
				throw new AuthorizationException("session expired");
			}
			if (session.isFinished()) {
				throw new InvalidStateException("Trying to access a finished session");
			}

			if(!session.getGameCode().equals(gameTxn.getGameCode())) {
				throw new BadRequestException("Gamecode mismatch");
			}

			if (!session.getPlayerId().equals(gameTxn.getPlayerId())) {
				throw new BadRequestException("Session and player mismatch");
			}

			sessionService.keepalive(session);
		}

		//PlayComplete stakes not allowed, as it can result in an edge case where cleardown is triggered, thus disallowing a cancel
		if(gameTxn.getTxnType().equals(TxnType.STAKE) && gameTxn.getPlayComplete().equals(true)) {
			throw new InvalidStateException("PlayComplete not supported for Stakes. Zero win required.");
		}

		//Does validation against suspended game in that case throws ForbiddenException
		final var game = gameService.getGame(gameTxn.getGameCode());

		platformIdentifierService.validateTxnRequestPrefixes(gameTxn);
		return txnService.process(gameTxn);
	}

	@PostMapping(path="/txn/cancel")
	public TxnReceipt cancelTxn(@RequestBody @Valid TxnCancelRequest cancelRequest) {
		return txnService.externalCancel(cancelRequest);
	}

	@PostMapping(path="/txn/{txnId}/recon/requeue")
	public TxnStatus requeueReconTxn(@PathVariable("txnId") String txnId, @RequestParam(required = false) Integer retryCount) {
		return manualReconService.requeueReconTxn(txnId, retryCount);
	}
}
