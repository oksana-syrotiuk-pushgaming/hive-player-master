package io.gsi.hive.platform.player.txn;

import javax.persistence.Entity;

@Entity(name = "t_txn")
public class Txn extends AbstractTxn {

	public Txn() {
		super();
	}
}