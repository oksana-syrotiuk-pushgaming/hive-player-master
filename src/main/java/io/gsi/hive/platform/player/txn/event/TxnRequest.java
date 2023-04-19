/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gsi.hive.platform.player.event.EventType;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TxnRequest extends TxnEvent {

    {
        type = EventType.txnRequest;
    }

    @NotBlank
    private String txnId;
    @NotBlank
    private String gameCode;
    @NotNull
    private String playId;
    @NotNull
    private Boolean playComplete;
    @NotNull
    private Boolean playCompleteIfCancelled;
    @NotNull
    private String roundId;
    @NotNull
    private Boolean roundComplete;
    @NotNull
    private Boolean roundCompleteIfCancelled;
    @NotBlank
    private String playerId;
    @NotNull
    private Boolean guest;
    @NotBlank
    private String igpCode;
    @NotNull
    private Mode mode;
    @NotBlank
    private String ccyCode;
    @NotNull
    private TxnType txnType;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amount;
    @DecimalMin("0.00")
    private BigDecimal jackpotAmount;//Jackpot amount allowed to be null for stakes, optional for wins
    private String sessionId;
    @Valid
    private BonusFundDetails bonusFundDetails;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> extraInfo;

    public TxnRequest() {
        super();
        amount = new BigDecimal("0.00");
    }

    //Quite a few copies used in txns now, so i've included a copy constructor
    public TxnRequest(TxnRequest txnRequest) {
        //Note timestamp not copied
        this();
        this.amount = txnRequest.amount;
        this.ccyCode = txnRequest.ccyCode;
        this.gameCode = txnRequest.gameCode;
        this.guest = txnRequest.guest;
        this.igpCode = txnRequest.igpCode;
        this.jackpotAmount = txnRequest.jackpotAmount;
        this.mode = Mode.valueOf(txnRequest.mode.name());
        this.playComplete = txnRequest.playComplete;
        this.playCompleteIfCancelled = txnRequest.playCompleteIfCancelled;
        this.playerId = txnRequest.playerId;
        this.playId = txnRequest.playId;
        this.roundComplete = txnRequest.roundComplete;
        this.roundCompleteIfCancelled = txnRequest.roundCompleteIfCancelled;
        this.roundId = txnRequest.roundId;
        this.sessionId = txnRequest.sessionId;
        this.txnId = txnRequest.txnId;
        this.txnType = TxnType.valueOf(txnRequest.txnType.name());
        this.extraInfo = txnRequest.extraInfo;
        this.bonusFundDetails = txnRequest.bonusFundDetails;
    }
}
