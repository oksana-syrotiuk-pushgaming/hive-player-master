/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.game;

import io.gsi.commons.exception.ForbiddenException;
import io.micrometer.core.annotation.Timed;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Setter
@Service
@ConfigurationProperties(prefix = "hive.game")
public class GameService {
	private Map<String, String> serviceCode = Map.of();
	private Set<String> disabled = Set.of();


	private String toServiceCode(String gameCode){
		return gameCode.split("-")[0];
	}

	private String resolveServiceCode(final String gameCode){
		return serviceCode.getOrDefault(gameCode, toServiceCode(gameCode));
	}

	private GameStatus resolveGameStatus(String gameCode){
		return disabled.contains(gameCode)
				? GameStatus.suspended
				: GameStatus.active;
	}

	@Timed
	@Cacheable(cacheNames="gameCache")
	public Game getGame(String gameCode){
		var game = new Game.GameBuilder()
				.code(gameCode)
				.serviceCode(resolveServiceCode(gameCode))
				.status(resolveGameStatus(gameCode))
				.build();

		if(!game.getStatus().equals(GameStatus.active)) {
			throw new ForbiddenException("Game suspended");
		}
		return game;
	}

}
