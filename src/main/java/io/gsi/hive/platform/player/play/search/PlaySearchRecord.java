package io.gsi.hive.platform.player.play.search;

import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.session.Mode;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.Data;

@Data
public class PlaySearchRecord {

    private String playId;
    private String playerId;
    private PlayStatus status;
    private Mode mode;
    private String gameCode;
    private boolean guest;
    private String ccyCode;
    private String igpCode;
    private ZonedDateTime createdAt;
    private ZonedDateTime modifiedAt;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private BigDecimal stake;
    private BigDecimal win;
    private String country;
    private Integer numTxns;
    private Boolean isFreeRound;
    private String playRef;
}
