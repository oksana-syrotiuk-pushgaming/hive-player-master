package io.gsi.hive.platform.player.txn.event;

import io.gsi.hive.platform.player.event.EventType;
import lombok.*;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TxnCleardownEvent extends TxnEvent {
    {
        type = EventType.txnCleardown;
    }

    @NotBlank
    private String txnId;
    @NotBlank
    private String cleardownTxnId;
    @NotBlank
    private BigDecimal amount;

}
