package io.gsi.hive.platform.player.mesh.presets;

import java.math.BigDecimal;

/**Presets which are'nt specific to any other class*/
public interface MeshGenericPresets 
{
	public final String LANG = "en";
	public final String COUNTRY = "GB";
	
	public final String MONETARYAMOUNT = "0.20";
	public final BigDecimal BDMONETARYAMOUNT = new BigDecimal("0.20");
	
	public final String HALFMONETARYAMOUNT = "0.10";
	public final BigDecimal BDHALFMONETARYAMOUNT = new BigDecimal("0.10");
	
	/**Zero value with correct (2) decimal places, for places where they arent ignored*/
	public final String ZERO = "0.00";
	/**Zero value with correct (2) decimal places, for places where they arent ignored*/
	public final BigDecimal BDZERO = new BigDecimal("0.00");
	
	/**Can be used if a generic string id is required*/
	public final String ID = "Id";
}
