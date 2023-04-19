package io.gsi.hive.platform.player.mesh.player;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import io.gsi.hive.platform.player.mesh.presets.MeshGenericPresets;
import io.gsi.hive.platform.player.mesh.presets.MeshPlayerIdPresets;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;
import io.gsi.hive.platform.player.mesh.wallet.MeshWalletBuilder;


public class MeshPlayerBuilder
{
	private String playerId = MeshPlayerIdPresets.DEFAULT;
	private String username = MeshPlayerIdPresets.USERNAME;
	private String alias = MeshPlayerIdPresets.ALIAS;
	private String country = MeshGenericPresets.COUNTRY;
	private String lang = MeshGenericPresets.LANG;
	private MeshWallet wallet;
	private MeshGender gender = MeshGender.Male;
	private LocalDate dateOfBirth = LocalDate.parse("1980-07-12");
	private Map<String, String> attributes;

	//A generic wallet will be used by default
	private MeshWalletBuilder walletBuilder;

	public MeshPlayerBuilder()
	{
		walletBuilder = new MeshWalletBuilder();
		wallet = walletBuilder.get();
		
		attributes = new HashMap<String, String>();
	}

	public MeshPlayerBuilder withPlayerId(String playerId)
	{
		this.playerId = playerId;
		return this;
	}
	
	public MeshPlayerBuilder withUsername(String username)
	{
		this.username = username;
		return this;
	}
	
	public MeshPlayerBuilder withAlias(String alias)
	{
		this.alias = alias;
		return this;
	}
	
	public MeshPlayerBuilder withCountry(String country)
	{
		this.country = country;
		return this;
	}
	
	public MeshPlayerBuilder withLang(String lang)
	{
		this.lang = lang;
		return this;
	}
	
	public MeshPlayerBuilder withWallet(MeshWallet wallet)
	{
		this.wallet = wallet;
		return this;
	}

	public MeshPlayerBuilder withGender(MeshGender gender) {
		this.gender = gender;
		return this;
	}

	public MeshPlayerBuilder withDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
		return this;
	}
	
	public MeshPlayerBuilder withAttributes(Map<String, String> attributes)
	{
		this.attributes = attributes;
		return this;
	}
	
	public MeshPlayer get()
	{
		return new MeshPlayer(playerId, username, alias, country, lang, wallet, gender, dateOfBirth, attributes);
	}
}
