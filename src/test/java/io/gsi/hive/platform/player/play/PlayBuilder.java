package io.gsi.hive.platform.player.play;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.MonetaryPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.session.Mode;

public class PlayBuilder {


	public static PlayBuilder play(){
		return new PlayBuilder();
	}

	private String playId;
	private String playerId;
	private PlayStatus status;
	private Mode mode;
	private String gameCode;
	private boolean guest;
	private String ccyCode;
	private String igpCode;
	private ZonedDateTime createdAt;
	private ZonedDateTime modifiedAt;
	private BigDecimal stake;
	private BigDecimal win;
	private Integer numTxns;
	private String playRef;
	private String bonusFundType;
	private String sessionId;

	private PlayBuilder() {
		this.playId = TxnPresets.PLAYID;
		this.playerId = PlayerPresets.PLAYERID;
		this.status = PlayStatus.FINISHED;
		this.mode = Mode.real;
		this.gameCode = GamePresets.CODE;
		this.guest = false;
		this.ccyCode = WalletPresets.CURRENCY;
		this.igpCode = IgpPresets.IGPCODE_IGUANA;
		this.createdAt = Instant.EPOCH.atZone(ZoneId.of("UTC"));
		this.modifiedAt = Instant.EPOCH.atZone(ZoneId.of("UTC")).plusHours(1l);
		this.stake = MonetaryPresets.BDAMOUNT;
		this.win = MonetaryPresets.BDHALFAMOUNT;
		this.numTxns = 2;
		this.playRef = null;
		this.bonusFundType = null;
		this.sessionId = null;
	}

	public PlayBuilder withPlayId(String playId) {
		this.playId=playId;
		return this;
	}
	public PlayBuilder withPlayerId(String playerId) {
		this.playerId=playerId;
		return this;
	}
	public PlayBuilder withStatus(PlayStatus status) {
		this.status=status;
		return this;
	}
	public PlayBuilder withMode(Mode mode) {
		this.mode=mode;
		return this;
	}
	public PlayBuilder withGameCode(String gameCode) {
		this.gameCode=gameCode;
		return this;
	}
	public PlayBuilder withGuest(boolean guest) {
		this.guest=guest;
		return this;
	}
	public PlayBuilder withCcyCode(String ccyCode) {
		this.ccyCode=ccyCode;
		return this;
	}
	public PlayBuilder withIgpCode(String igpCode) {
		this.igpCode=igpCode;
		return this;
	}
	public PlayBuilder withCreatedAt(ZonedDateTime createdAt) {
		this.createdAt=createdAt;
		return this;
	}
	public PlayBuilder withModifiedAt(ZonedDateTime modifiedAt) {
		this.modifiedAt=modifiedAt;
		return this;
	}
	public PlayBuilder withStake(BigDecimal stake) {
		this.stake=stake;
		return this;
	}
	public PlayBuilder withWin(BigDecimal win) {
		this.win=win;
		return this;
	}
	public PlayBuilder withNumTxns(Integer numTxns) {
		this.numTxns=numTxns;
		return this;
	}

	public PlayBuilder withPlayRef(String playRef) {
		this.playRef = playRef;
		return this;
	}

	public PlayBuilder withBonusFundType(String bonusFundType) {
		this.bonusFundType = bonusFundType;
		return this;
	}

	public PlayBuilder withSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public Play build(){
		Play play = new Play();
		play.setPlayId(playId);
		play.setCreatedAt(createdAt);
		play.setGameCode(gameCode);
		play.setGuest(guest);
		play.setIgpCode(igpCode);
		play.setMode(mode);
		play.setModifiedAt(modifiedAt);
		play.setNumTxns(numTxns);
		play.setPlayerId(playerId);
		play.setStake(stake);
		play.setStatus(status);
		play.setWin(win);
		play.setCcyCode(ccyCode);
		play.setPlayRef(playRef);
		play.setBonusFundType(bonusFundType);
		play.setSessionId(sessionId);
		return play;
	}


}
