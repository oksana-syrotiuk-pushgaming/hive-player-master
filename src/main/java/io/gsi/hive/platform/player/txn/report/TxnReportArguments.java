package io.gsi.hive.platform.player.txn.report;

import io.gsi.commons.validation.ValidCountry;
import io.gsi.hive.platform.player.txn.Mode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@AllArgsConstructor
public class TxnReportArguments
{
    private String playerId;
    private String username;
    private String gameCode;
    private Mode mode;
    private Boolean guest;
    private Boolean bonus;
    private String type;
    private String status;
    private String ccyCode;
    @ValidCountry
    private String country;
    @ValidTxnReportGroupBy
    private Set<TxnGroupBy> groupBy;
    private Set<TxnOrderBy> orderBy;
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateFrom;
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateTo;
    private List<String> igpCodes;
    private Boolean filterBigWins;
    
    public TxnReportArguments()
    {
        this.playerId = null;
        this.username = null;
        this.gameCode = null;
        this.mode = Mode.real;
        this.guest = Boolean.FALSE;
        this.type = null;
        this.status = null;
        this.ccyCode = null;
        this.groupBy = new HashSet<>();
        this.orderBy = new HashSet<>();
        this.igpCodes = new ArrayList<>();
        this.filterBigWins = false;
    }

  /***
   * request parameter : groupBy=ccy_code,country
   * Spring is responsible for de-serialization into Set
   * But spring fails to de-serialize directly into Set<TxnGroupBy>
   */
  public void setGroupBy(Set<String> groupBy)
  {
    this.groupBy = groupBy.stream()
        .map(TxnGroupBy::valueOf)
        .collect(Collectors.toSet());
  }

  public void setGroupByEnum(Set<TxnGroupBy> groupBy)
  {
    this.groupBy = groupBy;
  }

  public Set<TxnOrderBy> getOrderBy()
  {
    return orderBy;
  }

  /***
   * request parameter : orderBy=stake,num_plays
   * Spring is responsible for de-serialization into Set
   * But spring fails to de-serialize directly into Set<TxnOrderBy>
   */
  public void setOrderBy(Set<String> orderBy)
  {
    this.orderBy = orderBy.stream()
        .map(TxnOrderBy::valueOf)
        .collect(Collectors.toSet());
  }

  public void setOrderByEnum(Set<TxnOrderBy> orderBy)
  {
    this.orderBy = orderBy;
  }
}
