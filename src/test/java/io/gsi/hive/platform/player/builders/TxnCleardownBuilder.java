package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.MonetaryPresets;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.TxnCleardown;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class TxnCleardownBuilder {

  private String txnId = TxnPresets.TXNID;
  private ZonedDateTime txnTs = TimePresets.ZONEDEPOCHUTC;
  private String cleardownTxnId = TxnPresets.TXNID;
  private BigDecimal amount = MonetaryPresets.BDAMOUNT;

  private TxnCleardownBuilder() {

  }

  public static TxnCleardownBuilder txn() {
    return new TxnCleardownBuilder();
  }

  public TxnCleardownBuilder withTxnId(String txnId) {
    this.txnId = txnId;
    return this;
  }

  public TxnCleardownBuilder withAmount(BigDecimal amount) {
    this.amount = amount;
    return this;
  }

  public TxnCleardownBuilder withCleardownTxnId(String cleardownTxnId) {
    this.cleardownTxnId = cleardownTxnId;
    return this;
  }

  public TxnCleardown build() {
    TxnCleardown txn = new TxnCleardown();
    txn.setTxnId(txnId);
    txn.setTxnTs(txnTs);
    txn.setCleardownTxnId(cleardownTxnId);
    txn.setAmount(amount);
    return txn;
  }


}
