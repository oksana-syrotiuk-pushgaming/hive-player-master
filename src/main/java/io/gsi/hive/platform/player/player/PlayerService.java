/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.gsi.commons.exception.AuthorizationException;
import io.micrometer.core.annotation.Timed;

@Service
public class PlayerService {

	private final PlayerRepository playerRepository;

	public PlayerService(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
	}

	@Transactional
	@Timed
	public void save(Player player) {
		playerRepository.saveAndFlush(player);
	}

	@Timed
	public Player get(PlayerKey playerKey) {
		return playerRepository.findById(playerKey).orElseThrow(() -> new AuthorizationException("Cannot find player: " + playerKey));
	}
}
