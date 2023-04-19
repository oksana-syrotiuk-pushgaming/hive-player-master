/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.player;

import io.gsi.commons.validation.ValidCountry;
import io.gsi.commons.validation.ValidLang;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.springframework.lang.Nullable;

@Entity(name="t_player")
@IdClass(PlayerKey.class)
public class Player {

	@Id @Column(name="player_id") @NotNull @Length(min=1,max=250)
	private String playerId;
	@Id @Column(name="igp_code") @NotNull @Length(min=1,max=12)
	private String igpCode;
	@Column(name="ccy_code") @NotNull
	private String ccyCode;
	@Column @Length(min=1,max=64)
	private String username;
	@Column @Length(min=1,max=64)
	private String alias;
	@Column @NotNull
	private Boolean guest;
	@Column @ValidCountry
	private String country;
	@Column @NotNull @ValidLang
	private String lang;

	@Transient @Nullable
	private transient String jurisdiction;

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

	public String getCcyCode() {
		return ccyCode;
	}

	public void setCcyCode(String ccyCode) {
		this.ccyCode = ccyCode;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = null;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Boolean getGuest() {
		return guest;
	}

	public void setGuest(Boolean guest) {
		this.guest = guest;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getJurisdiction() { return jurisdiction; }

	public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

	public boolean update(Player player) {
		// TODO check for currency change and error if it is different
		boolean changed = false;
		if (!this.username.equals(player.username)) {
			this.username = player.username;
			changed = true;
		}
		if (!this.alias.equals(player.alias)) {
			this.alias = player.alias;
			changed = true;
		}
		if (!this.lang.equals(player.lang)) {
			this.lang = player.lang;
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Player player = (Player) o;
		return Objects.equals(playerId, player.playerId) &&
				Objects.equals(igpCode, player.igpCode) &&
				Objects.equals(ccyCode, player.ccyCode) &&
				Objects.equals(username, player.username) &&
				Objects.equals(alias, player.alias) &&
				Objects.equals(guest, player.guest) &&
				Objects.equals(country, player.country) &&
				Objects.equals(lang, player.lang);
	}

	@Override
	public int hashCode() {
		return Objects.hash(playerId, igpCode, ccyCode, username, alias, guest, country, lang);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Player{");
		sb.append("playerId='").append(playerId).append('\'');
		sb.append(", igpCode='").append(igpCode).append('\'');
		sb.append(", ccyCode='").append(ccyCode).append('\'');
		sb.append(", username='").append(username).append('\'');
		sb.append(", alias='").append(alias).append('\'');
		sb.append(", guest=").append(guest);
		sb.append(", country='").append(country).append('\'');
		sb.append(", lang='").append(lang).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
