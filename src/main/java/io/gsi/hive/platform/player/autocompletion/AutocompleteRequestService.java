package io.gsi.hive.platform.player.autocompletion;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.game.Game;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.play.PlayService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PessimisticLockException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "hive.autocomplete.enabled", havingValue = "true", matchIfMissing = true)
public class AutocompleteRequestService {
	private static final Log logger = LogFactory.getLog(AutocompleteRequestService.class);

	private final AutocompleteGameEndpoint endpoint;
	private final AutocompleteRequestRepository autocompleteRequestRepository;
	private final DiscoveryClient discoveryClient;
	private final GameService gameService;
	private final PlayService playService;
	private final Integer requestRetries;

	public AutocompleteRequestService(GameService gameService, DiscoveryClient discoveryClient, AutocompleteGameEndpoint endpoint,
									  AutocompleteRequestRepository repository, PlayService playService, @Value("${hive.autocomplete.requestRetries:4}") Integer retries) {
		this.gameService = gameService;
		this.discoveryClient = discoveryClient;
		this.endpoint = endpoint;
		this.autocompleteRequestRepository = repository;
		this.playService = playService;
		this.requestRetries = retries;
	}

	private void routeRequest(AutocompleteRequest autocompleteRequest) {
		logger.info("Sending AutoComplete Request: " + autocompleteRequest.toString());
		endpoint.performAutocomplete(
				autocompleteRequest,
				getGameServiceName(autocompleteRequest.getGameCode()),
				autocompleteRequest.getPlayId()
		);
	}

	private String getGameServiceName(String gameCode)
	{
		List<String> allGameServiceNames = discoveryClient.getServices();
		Game game = gameService.getGame(gameCode);
		String gameServiceCode = game.getServiceCode();

		List<String> gameServiceNames = allGameServiceNames.stream()
				.filter(service -> service.contains(
						"hive-game-" + gameServiceCode + "-service"))
				.collect(Collectors.toList());

		if(gameServiceNames.size() > 1 || gameServiceNames.isEmpty())
		{
			throw new InvalidStateException("Cannot find game service to send autocomplete requests to");
		}

		return gameServiceNames.get(0);
	}

	@Transactional
	public void sendRequest(String playId) {

		//TODO: double read with last service could likely be improved
		AutocompleteRequest request = null;
		try{
			request = autocompleteRequestRepository.findAndLockByPlayId(playId);
			if(request == null){
				//Already been processed and removed
				return;
			}
		}
		//Task already locked, can safely ignore
		catch(PessimisticLockException e){
			return;
		}
		try {
			routeRequest(request);
			playService.markPlayAsAutocompleted(playId);
			autocompleteRequestRepository.deleteById(playId);
		}

		//Some error sending callback - increment retries or set to recon if exceeded
		catch(Exception e){
			logger.error("Autocompletion request failed: ", e);
			if (request.getRetries() >= requestRetries){
				logger.info("Autocompletion: Request Exceeding retries, playId: " + playId + ", gameCode:" + request.getGameCode());
			}
			request.setException(e.getClass().getSimpleName());
			request.setRetries(request.getRetries() + 1);
			autocompleteRequestRepository.save(request);
		}
	}
}
