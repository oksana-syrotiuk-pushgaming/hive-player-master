package io.gsi.hive.platform.player.play;

import io.gsi.hive.platform.player.persistence.converter.UTCDateTimeAttributeConverter;
import io.gsi.hive.platform.player.session.Mode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@Entity(name="t_play")
public class Play {
    @Id
    @Column(name="play_id") @Length(min=1,max=64) @NotNull
    private String playId;
    @Column(name="player_id") @NotNull
    private String playerId;
    @Enumerated(EnumType.STRING) @NotNull
    private PlayStatus status;
    @NotNull @Enumerated(EnumType.STRING)
    private Mode mode;
    @Column(name="game_code") @NotNull
    private String gameCode;
    @Column(name="guest") @NotNull
    private boolean guest;
    @Column(name="auto_completed")
    private Boolean autoCompleted;
    @Column(name="ccy_code") @NotNull
    private String ccyCode;
    @Column(name="igp_code") @NotNull
    private String igpCode;
    @Column(name="created_at") @Convert(converter=UTCDateTimeAttributeConverter.class) @NotNull
    private ZonedDateTime createdAt;
    @Column(name="modified_at") @Convert(converter=UTCDateTimeAttributeConverter.class) @NotNull
    private ZonedDateTime modifiedAt;
    @NotNull
    private BigDecimal stake;
    @NotNull
    private BigDecimal win;
    @Column(name="num_txns")@NotNull
    private Integer numTxns;
    @Column(name="play_ref")
    private String playRef;
    @Column(name="bonus_fund_type")
    private String bonusFundType;
    @Column(name="session_id")
    private String sessionId;
    @Column(name="cleardown")
    private BigDecimal cleardownAmount;
}
