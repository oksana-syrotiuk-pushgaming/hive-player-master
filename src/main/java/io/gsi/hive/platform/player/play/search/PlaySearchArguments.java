package io.gsi.hive.platform.player.play.search;

import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.session.Mode;
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
public class PlaySearchArguments {

    @Min(0)
    private Integer page;
    @Min(1)
    private Integer pageSize;

    private String playId;
    private String playerId;
    private PlayStatus status;
    private Mode mode;
    private String gameCode;
    @NotNull
    private Boolean guest;
    private String ccyCode;
    private List<String> igpCodes;

    private String playRef;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateFrom;
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateTo;

    public PlaySearchArguments() {
        this.page = 0;
        this.pageSize = 100;
        this.mode = Mode.real;
        this.guest = false;
    }
}
