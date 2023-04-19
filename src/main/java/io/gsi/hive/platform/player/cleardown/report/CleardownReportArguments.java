package io.gsi.hive.platform.player.cleardown.report;

import static io.gsi.hive.platform.player.play.report.PlayGroupBy.ccy_code;

import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.play.report.PlayGroupBy;
import io.gsi.hive.platform.player.session.Mode;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.GroupSequence;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@AllArgsConstructor
@GroupSequence({CleardownReportArguments.class, CleardownReportArguments.Validator.class})
@CleardownReportArgumentsConstraint(groups = CleardownReportArguments.Validator.class)
public class CleardownReportArguments {

  private String playerId;
  private PlayStatus status;
  private Mode mode;
  private String gameCode;
  private Boolean guest;
  private String ccyCode;
  private String country;
  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private ZonedDateTime dateFrom;
  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private ZonedDateTime dateTo;
  @NotEmpty
  private Set<PlayGroupBy> groupBy = new HashSet<>();
  private Set<CleardownOrderBy> orderBy = new HashSet<>();
  private List<String> igpCodes;

  public CleardownReportArguments() {
    this.mode = Mode.real;
    this.guest = false;
  }

  public void setGroupBy(Set<String> groupBy) {
    this.groupBy = groupBy.stream()
        .map(PlayGroupBy::valueOf)
        .collect(Collectors.toSet());
  }

  public void setGroupByEnum(Set<PlayGroupBy> groupBy) {
    this.groupBy = groupBy;
  }

  public Set<CleardownOrderBy> getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(Set<String> orderBy) {
    this.orderBy = orderBy.stream()
        .map(CleardownOrderBy::valueOf)
        .collect(Collectors.toSet());
  }

  public void setOrderByEnum(Set<CleardownOrderBy> orderBy) {
    this.orderBy = orderBy;
  }


  public static class Validator
      implements ConstraintValidator<CleardownReportArgumentsConstraint, CleardownReportArguments> {

    @Override
    public void initialize(CleardownReportArgumentsConstraint txnConstraint) {
      //NOT IMPLEMENTED
    }

    @Override
    public boolean isValid(CleardownReportArguments arguments, ConstraintValidatorContext context) {
      return arguments.getGroupBy().contains(ccy_code);
    }
  }

}
