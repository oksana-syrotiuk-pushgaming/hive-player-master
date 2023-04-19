/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gsi.commons.validation.ValidCountry;
import io.gsi.commons.validation.ValidLang;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshPlayer {

	@NotNull @Size(max=250)
	private String playerId;
	@Size(max=64)
	private String username;
	@JsonInclude(Include.NON_NULL)
	@Size(max=64)
	private String alias;
	@NotNull @ValidCountry
	private String country;
	@NotNull @ValidLang
	private String lang;
	@NotNull @Valid
	private MeshWallet wallet;
	private Map<String, String> attributes;

	private MeshGender gender;
	private LocalDate dateOfBirth;

	public MeshPlayer() {}

	@JsonCreator
	public MeshPlayer(
			@JsonProperty("playerId") String playerId,
			@JsonProperty("username") String username,
			@JsonProperty("alias") String alias,
			@JsonProperty("country") String country,
			@JsonProperty("lang") String lang,
			@JsonProperty("wallet") MeshWallet wallet,
			@JsonProperty("gender") MeshGender gender,
			@JsonProperty("dateOfBirth") LocalDate dateOfBirth,
			@JsonProperty("attributes") Map<String, String> attributes) {
		this.playerId = playerId;
		this.username = username;
		this.alias = alias;
		this.country = country;
		this.lang = lang;
		this.wallet = wallet;
		this.gender = gender;
		this.dateOfBirth = dateOfBirth;
		this.attributes = attributes;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
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

	public MeshWallet getWallet() {
		return wallet;
	}

	public void setWallet(MeshWallet wallet) {
		this.wallet = wallet;
	}

	public MeshGender getGender() {
		return this.gender;
	}

	public void setGender(MeshGender gender) {
		this.gender = gender;
	}

	public LocalDate getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return "Player{" +
				"playerId='" + playerId + '\'' +
				", username='" + username + '\'' +
				", alias='" + alias + '\'' +
				", country='" + country + '\'' +
				", lang='" + lang + '\'' +
				", wallet=" + wallet +
				", gender=" + gender +
				", dateOfBirth=" + dateOfBirth +
				", attributes=" + attributes +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MeshPlayer player = (MeshPlayer) o;
		return Objects.equals(playerId, player.playerId) &&
				Objects.equals(username, player.username) &&
				Objects.equals(alias, player.alias) &&
				Objects.equals(country, player.country) &&
				Objects.equals(lang, player.lang) &&
				Objects.equals(wallet, player.wallet) &&
				Objects.equals(gender, player.gender) &&
				Objects.equals(dateOfBirth, player.dateOfBirth) &&
				Objects.equals(attributes, player.attributes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(playerId, username, alias, country, lang, wallet, gender, dateOfBirth, attributes);
	}
}
