package io.gsi.hive.platform.player.txn;

import io.gsi.hive.platform.player.persistence.converter.UTCDateTimeAttributeConverter;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Entity(name = "t_txn_cleardown")
public class TxnCleardown {

    @Id
    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "txn_ts") @Convert(converter= UTCDateTimeAttributeConverter.class)
    private ZonedDateTime txnTs;

    @Column(name = "cleardown_txn_id")
    private String cleardownTxnId;

    @Column
    private BigDecimal amount;

}
