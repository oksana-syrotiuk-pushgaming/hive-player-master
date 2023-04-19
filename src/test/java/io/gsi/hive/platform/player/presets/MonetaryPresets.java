package io.gsi.hive.platform.player.presets;

import java.math.BigDecimal;

public interface MonetaryPresets {

	static String CCYCODE = "GBP";
	static BigDecimal BDAMOUNT = new BigDecimal("20.00");
	static String STRAMOUNT = "20.00";
	static int INTAMOUNT = 20;
	
	static BigDecimal BDHALFAMOUNT = new BigDecimal("10.00");
	static String STRHALFAMOUNT = "10.00";
	static int INTHALFAMOUNT = 10;
}
