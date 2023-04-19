package io.gsi.hive.platform.player.play.report;

import static io.gsi.hive.platform.player.play.report.PlayGroupBy.ccy_code;

import io.gsi.hive.platform.player.play.PlayStatus;
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
@GroupSequence({PlayReportArguments.class,PlayReportArguments.Validator.class})
@PlayReportArgumentsConstraint(groups=PlayReportArguments.Validator.class)
public class PlayReportArguments {

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
    private Set<PlayOrderBy> orderBy = new HashSet<>();
    private List<String> igpCodes;
    private Boolean onlyFreeroundPlays;
    
    public PlayReportArguments() {
        this.mode = Mode.real;
        this.guest = false;
    }

  /***
   * request parameter : groupBy=ccy_code,country
   * Spring is responsible for de-serialization into Set
   * But spring fails to de-serialize directly into Set<PlayGroupBy>
   */
    public void setGroupBy(Set<String> groupBy) {
      this.groupBy = groupBy.stream()
          .map(PlayGroupBy::valueOf)
          .collect(Collectors.toSet());
    }

  public void setGroupByEnum(Set<PlayGroupBy> groupBy) {
    this.groupBy = groupBy;
  }

    public Set<PlayOrderBy> getOrderBy() {
        return orderBy;
    }

  /***
   * request parameter : orderBy=stake,num_plays
   * Spring is responsible for de-serialization into Set
   * But spring fails to de-serialize directly into Set<PlayOrderBy>
   */
    public void setOrderBy(Set<String> orderBy) {
      this.orderBy = orderBy.stream()
          .map(PlayOrderBy::valueOf)
          .collect(Collectors.toSet());
    }

  public void setOrderByEnum(Set<PlayOrderBy> orderBy) {
    this.orderBy = orderBy;
  }

    /*
	 * Implements complex validation for the PlayerReportArguments object
	 */
	public static class Validator
	implements ConstraintValidator<PlayReportArgumentsConstraint,PlayReportArguments> {
		@Override
		public void initialize(PlayReportArgumentsConstraint txnConstraint) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean isValid(PlayReportArguments arguments, ConstraintValidatorContext context) {
        return arguments.getGroupBy().contains(ccy_code);
    }
	}

}
