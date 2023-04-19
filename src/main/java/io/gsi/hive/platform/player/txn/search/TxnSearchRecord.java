package io.gsi.hive.platform.player.txn.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TxnSearchRecord
{
    private String txnId;
    private String gameCode;
    private String playId;
    private boolean playComplete;
    private boolean playCompleteIfCancelled;
    private String roundId;
    private boolean roundComplete;
    private boolean roundCompleteIfCancelled;
    private String playerId;
    private String username;
    private String country;
    private String igpCode;
    private String sessionId;
    private Mode mode;
    private boolean guest;
    private boolean bonus;
    private String ccyCode;
    private TxnType type;
    private BigDecimal amount;
    private BigDecimal jackpotAmount;
    private ZonedDateTime txnTs;
    private ZonedDateTime cancelTs;
    private String txnRef;
    private TxnStatus status;
    private String accessToken;
}
