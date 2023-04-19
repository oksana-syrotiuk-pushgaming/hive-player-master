package io.gsi.hive.platform.player.demo.builders;

import io.gsi.hive.platform.player.demo.wallet.GuestWalletCreate;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;

public class GuestWalletCreateBuilder {

	private String playerId = PlayerPresets.PLAYERID;
	private String igpCode = IgpPresets.IGPCODE_IGUANA;
	private Boolean guest = true;
	private String gameCode = GamePresets.CODE;
	private String ccyCode = WalletPresets.CURRENCY;

	public static GuestWalletCreateBuilder guestWalletCreate(){
		return new GuestWalletCreateBuilder();
	}
	
	public GuestWalletCreateBuilder withPlayerId(String playerId) {
		this.playerId=playerId;
		return this;
	}
	public GuestWalletCreateBuilder withIgpCode(String igpCode) {
		this.igpCode=igpCode;
		return this;
	}
	public GuestWalletCreateBuilder withGuest(Boolean guest) {
		this.guest=guest;
		return this;
	}
	public GuestWalletCreateBuilder withGameCode(String gameCode) {
		this.gameCode=gameCode;
		return this;
	}
	public GuestWalletCreateBuilder withCcyCode(String ccyCode) {
		this.ccyCode=ccyCode;
		return this;
	}

	public GuestWalletCreate build(){
		GuestWalletCreate guestWalletCreate = new GuestWalletCreate();
		
		guestWalletCreate.setPlayerId(playerId);
		guestWalletCreate.setIgpCode(igpCode);
		guestWalletCreate.setGuest(guest);
		guestWalletCreate.setGameCode(gameCode);
		guestWalletCreate.setCcyCode(ccyCode);
		
		return guestWalletCreate;
	}
}
