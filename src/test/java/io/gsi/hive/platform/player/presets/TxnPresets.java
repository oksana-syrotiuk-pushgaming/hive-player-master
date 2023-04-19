package io.gsi.hive.platform.player.presets;

import io.gsi.hive.platform.player.txn.TxnType;

public interface TxnPresets {
	String TXNID = "1000-1";
	String TXNID_2 = "1000-2";
	String PLAYID = "1000-10";
	String PLAYID_2 = "1000-12";
	String PLAYID_3 = "1000-13";
	String PLAYID_4 = "1000-14";
	String ROUNDID = "1000-10";
	String TXNREF = "1000-1";
	String TXNREF2 = "1000-2";
	String PLAYREF_NULL = null;
	String PLAYREF = "111111";
	String PLAYREF2 = "111112";
	String PLATFORMID = "9999";
	String EXCEPTION = "InternalServerException";
	TxnType TYPE = TxnType.STAKE;
	String ACCESSTOKEN = "123";
	String GAMECODE = "fatrabbit-01";
}

