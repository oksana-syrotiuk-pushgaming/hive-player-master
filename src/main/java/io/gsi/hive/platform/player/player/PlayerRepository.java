/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.player;

import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PlayerRepository extends JpaRepository<Player,PlayerKey> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Player findAndLockByPlayerIdAndIgpCodeAndGuest(String playerId, String igpCode, boolean guest);

	Player findByPlayerIdAndIgpCode(String playerId, String igpCode);
}
