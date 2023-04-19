/**
 * © gsi.io 2018
 */
package io.gsi.hive.platform.player.demo.wallet;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.gsi.hive.platform.player.wallet.Fund;

public class GuestWallet {
	@NotNull
	private Long walletId;
	@NotNull @Size(min=1,max=250)
	private String playerId;
	@NotNull @Size(min=1,max=32)
	private String igpCode;
	@NotNull
	private Boolean guest;
	@Size(min=1,max=64)
	private String gameCode;
	@NotNull
	private String ccyCode;
	@NotNull
	private ZonedDateTime created;
	@NotNull @Digits(integer=16,fraction=2) @DecimalMin("0.00")
	private BigDecimal balance;
	@NotNull
	private List<Fund> funds;

	public Long getWalletId() {
		return walletId;
	}

	public void setWalletId(Long walletId) {
		this.walletId = walletId;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getIgpCode() {
		return igpCode;
	}

	public void setIgpCode(String igpCode) {
		this.igpCode = igpCode;
	}

	public Boolean getGuest() {
		return guest;
	}

	public void setGuest(Boolean guest) {
		this.guest = guest;
	}

	public String getGameCode() {
		return gameCode;
	}

	public void setGameCode(String gameCode) {
		this.gameCode = gameCode;
	}

	public String getCcyCode() {
		return ccyCode;
	}

	public void setCcyCode(String ccyCode) {
		this.ccyCode = ccyCode;
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public List<Fund> getFunds() {
		return funds;
	}

	public void setFunds(List<Fund> funds) {
		this.funds = funds;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuestWallet{walletId=‘").append(walletId).append("’, playerId=‘").append(playerId)
				.append("’, igpCode=‘").append(igpCode).append("’, guest=‘").append(guest).append("’, gameCode=‘")
				.append(gameCode).append("’, ccyCode=‘").append(ccyCode).append("’, created=‘").append(created)
				.append("’, balance=‘").append(balance).append("’, funds=‘").append(funds).append("}");
		return builder.toString();
	}

	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!getClass().equals(other.getClass())) {
			return false;
		}
		GuestWallet castOther = (GuestWallet) other;
		return Objects.equals(walletId, castOther.walletId) && Objects.equals(playerId, castOther.playerId)
				&& Objects.equals(igpCode, castOther.igpCode) && Objects.equals(guest, castOther.guest)
				&& Objects.equals(gameCode, castOther.gameCode) && Objects.equals(ccyCode, castOther.ccyCode)
				&& Objects.equals(created, castOther.created) && Objects.equals(balance, castOther.balance)
				&& Objects.equals(funds, castOther.funds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(walletId, playerId, igpCode, guest, gameCode, ccyCode, created, balance, funds);
	}
}
