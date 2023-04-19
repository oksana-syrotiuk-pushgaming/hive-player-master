package io.gsi.hive.platform.player.recon.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.gsi.commons.monitoring.ExceptionMonitorService;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnEvent;
import io.gsi.hive.platform.player.txn.event.TxnRequest;


/**
 * TODO leader election to avoid duplication of work
 *
 * This class should get pendings from all upstream games and pass it to the integration service*
 * Will eventualy need leader election to avoid work duplication
 **/
@Service
@ConditionalOnProperty(name = "hive.recon.enabled", havingValue = "true", matchIfMissing = true)
public class ReconGamePollingService {

	private static final Log logger = LogFactory.getLog(ReconGamePollingService.class);

	private final ReconGameEndpoint endpoint;
	private final ReconTxnIntegrationService integrationService;
	private final DiscoveryClient discoveryClient;
	private final int pendingTxnFetchLimit = 200;
	private final int pendingTxnFetchMinAgeMinute = 2;
	private final ExceptionMonitorService exceptionMonitorService;

	public ReconGamePollingService(ReconTxnIntegrationService service, ReconGameEndpoint endpoint, DiscoveryClient discoveryClient, ExceptionMonitorService exceptionMonitorService) {
		this.integrationService = service;
		this.endpoint = endpoint;
		this.discoveryClient = discoveryClient;
		this.exceptionMonitorService = exceptionMonitorService;
	}

	@Scheduled(initialDelayString = "${hive.recon.gamepoll.initialDelay:90000}", fixedDelayString = "${hive.recon.gamepoll.fixedDelay:120000}")
	public void fetchAndIntegratePendingGameTxnEvents()
	{
		logger.info("recon polling games for unreceived Txns");
		List<TxnEvent> txnEvents = new ArrayList<>();
		List<String> services = discoveryClient.getServices();
		if(services.isEmpty()) {
			//This message suggests an issue with service discovery
			logger.info("Integration: No services found");
		}
		else {
			services.stream()
			.filter(isHiveGame()).forEach(
					serviceName -> {
						txnEvents.addAll(retrieveTxnEvents(serviceName));
					});

			if(txnEvents.isEmpty()) {
				logger.info("Integration: No Pending Txns from any game");
			}
			else {
				integrateTxns(txnEvents);
			}
		}
	}

	private List<TxnEvent> retrieveTxnEvents(String gameServiceName) {
		String url = new StringBuilder("http://")
				.append(gameServiceName)
				.append("/hive/s2s/pendingtxnevents")
				.append("?limit=")
				.append(pendingTxnFetchLimit)
				.append("&minAgeMinutes=")
				.append(pendingTxnFetchMinAgeMinute)
				.toString();
		try {
			return endpoint.getUpstreamPendingTxns(url);
		} catch (RuntimeException ex) {
			exceptionMonitorService.monitorException(ex);
			logger.error("failed to retrieve txn events from game service " + gameServiceName + " : " + ex.getMessage());
			return Arrays.asList();
		}
	}

	private void integrateTxns(List<TxnEvent> requests)
	{
		for(TxnEvent request : requests)
		{
			try {
				if (request instanceof TxnRequest) {
					integrationService.integrateGameTxn((TxnRequest) request);
				} else if (request instanceof TxnCancelRequest) {
					integrationService.integrateCancelTxn((TxnCancelRequest) request);
				}
			} catch (RuntimeException ex) {
				exceptionMonitorService.monitorException(ex);
				logger.error("failed to integrate txn event " + request + ", With Error: " + ex.getClass().getSimpleName() + ", Message: " + ex.getMessage());
			}
		}		
	}

	private Predicate<String> isHiveGame() {
		return service -> service.contains("hive-game-");
	}

}
