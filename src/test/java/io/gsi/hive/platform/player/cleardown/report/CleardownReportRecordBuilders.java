package io.gsi.hive.platform.player.cleardown.report;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.MonetaryPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;

public class CleardownReportRecordBuilders {

	static public CleardownReportRecord.CleardownReportRecordBuilder defaultCleardownReportRecordBuilder() {
		return CleardownReportRecord.builder()
				.ccyCode(PlayerPresets.CCY_CODE)
				.gameCode(GamePresets.CODE)
				.country(PlayerPresets.COUNTRY)
				.numCleardowns(1L)
				.uniquePlayers(1L)
				.totalWin(MonetaryPresets.BDHALFAMOUNT)
				.igpCode(IgpPresets.IGPCODE_IGUANA);

	}
}

