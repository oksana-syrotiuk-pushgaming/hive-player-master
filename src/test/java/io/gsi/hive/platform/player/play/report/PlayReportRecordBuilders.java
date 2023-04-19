package io.gsi.hive.platform.player.play.report;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.MonetaryPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;

public class PlayReportRecordBuilders {

	static public PlayReportRecord.PlayReportRecordBuilder defaultPlayReportRecordBuilder() {
		return PlayReportRecord.builder()
				.ccyCode(PlayerPresets.CCY_CODE)
				.gameCode(GamePresets.CODE)
				.country(PlayerPresets.COUNTRY)
				.numPlays(1L)
				.uniquePlayers(1L)
				.totalStake(MonetaryPresets.BDAMOUNT)
				.totalWin(MonetaryPresets.BDHALFAMOUNT)
				.grossGamingRevenue(MonetaryPresets.BDAMOUNT.subtract(MonetaryPresets.BDHALFAMOUNT))
				.igpCode(IgpPresets.IGPCODE_IGUANA);

	}
}

