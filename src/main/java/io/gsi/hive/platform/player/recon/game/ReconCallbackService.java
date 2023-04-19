package io.gsi.hive.platform.player.recon.game;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.PessimisticLockException;

import io.gsi.hive.platform.player.game.GameService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.txn.TxnCallback;
import io.gsi.hive.platform.player.txn.TxnService;

@Service
@ConditionalOnProperty(name = "hive.recon.enabled", havingValue = "true", matchIfMissing = true)
public class ReconCallbackService {
	private static final Log logger = LogFactory.getLog(TxnService.class);

	private final ReconGameEndpoint endpoint;
	private final Integer callbackRetries;
	private final TxnCallbackRepository callbackRepository;
	private final DiscoveryClient discoveryClient;
	private final GameService gameService;

	public ReconCallbackService(GameService gameService, DiscoveryClient discoveryClient, ReconGameEndpoint endpoint, TxnCallbackRepository repository, @Value("${hive.recon.callbackRetries:4}") Integer retries) {
		this.gameService = gameService;
		this.discoveryClient = discoveryClient;
		this.endpoint = endpoint;
		this.callbackRepository = repository;
		this.callbackRetries = retries;
	}

	private void routeCallback(TxnCallback callbackTxn) {

		String url = new StringBuilder("http://")
				.append(getGameServiceName(callbackTxn.getGameCode()))
				.append("/hive/s2s/txn/")
				.append(callbackTxn.getTxnId())
				.append("/afterReconciliation")
				.toString();

		endpoint.sendCallback(url, callbackTxn);
	}

	private String getGameServiceName(String gameCode)
	{
		List<String> gameServiceNames = discoveryClient.getServices().stream()
				.filter(service -> service.contains(
						"hive-game-" + gameService.getGame(gameCode).getServiceCode() + "-service"))
				.collect(Collectors.toList());

		if(gameServiceNames.size() > 1 || gameServiceNames.isEmpty())
		{
			throw new InvalidStateException("Cannot find game service to callback to");
		}

		return gameServiceNames.get(0);
	}

	@Transactional
	public void sendCallback(String txnId) {

		TxnCallback callback = null;
		try{
			callback = callbackRepository.findAndLockByTxnId(txnId);
			if(callback == null){
				//Already been processed and removed
				return;
			}
		}
		//Task already locked, can safely ignore
		catch(PessimisticLockException e){
			return;
		}
		try {
			routeCallback(callback);

			callbackRepository.deleteFromCallbackQueue(txnId);
		}

		//Some error sending callback - increment retries or set to recon if exceeded
		catch(Exception e){
			if (callback.getRetries() >= callbackRetries){
				logger.info("Recon: Callback Exceeding retries: " + txnId);
			}
			callbackRepository.incrementRetriesAndSetException(txnId, e.getClass().getSimpleName());
		}
		return;
	}
}
