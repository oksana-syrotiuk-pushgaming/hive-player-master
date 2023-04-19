package io.gsi.hive.platform.player.txn.search;

import io.gsi.commons.validation.ValidCountry;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@AllArgsConstructor
public class TxnSearchArguments
{
    @Min(0)
    private Integer page;
    @Min(1)
    private Integer pageSize;
    private String playerId;
    private String username;
    private String gameCode;
    private Mode mode;
    private boolean guest;
    private Boolean onlyFreeroundTxns;
    private TxnType type;
    private TxnStatus status;
    private String ccyCode;
    @ValidCountry
    private String country;
    private String txnId;
    private String playId;
    private List<String> igpCodes;
    private String cacheBuster;
    private String txnRef;
    private String playRef;
    private String accessToken;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateFrom;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateTo;

    public TxnSearchArguments()
    {
        this.page = 0;
        this.pageSize = 100;
        this.mode = Mode.real;
    }

}
