package io.gsi.hive.platform.player.txn;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.MonetaryPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.txn.event.TxnRequest.TxnRequestBuilder;

public class TxnRequestPresets {

    public static TxnRequestBuilder defaultStakeTxnRequestBuilder() {
        return TxnRequest.builder()
                .txnId(TxnPresets.TXNID)
                .playId(TxnPresets.PLAYID)
                .roundId(TxnPresets.ROUNDID)
                .gameCode(GamePresets.CODE)
                .playComplete(false)
                .roundComplete(false)
                .playCompleteIfCancelled(true)
                .roundCompleteIfCancelled(true)
                .playerId(PlayerPresets.PLAYERID)
                .igpCode(IgpPresets.IGPCODE_IGUANA)
                .mode(Mode.real)
                .guest(false)
                .ccyCode(MonetaryPresets.CCYCODE)
                .sessionId(SessionPresets.SESSIONID)
                .txnType(TxnType.STAKE)
                .amount(MonetaryPresets.BDAMOUNT)
                .jackpotAmount(MonetaryPresets.BDHALFAMOUNT);
    }

    public static TxnRequestBuilder defaultWinTxnRequestBuilder() {
        return defaultStakeTxnRequestBuilder()
                .txnType(TxnType.WIN)
                .playComplete(true)
                .roundComplete(true);
    }

    public static TxnRequestBuilder defaultBonusTxnRequestBuilder() {
		return defaultStakeTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build());
	}
}
