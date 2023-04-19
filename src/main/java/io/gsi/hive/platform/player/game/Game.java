/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {
	private String code;
	@Builder.Default
	private GameStatus status = GameStatus.active;
	private String serviceCode;
}
