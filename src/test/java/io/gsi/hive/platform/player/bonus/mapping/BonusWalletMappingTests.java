package io.gsi.hive.platform.player.bonus.mapping;

import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.event.EventType;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.event.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BonusWalletMappingTests {

	@Test
	public void txnToRequest() {
		Txn txn = TxnBuilder.txn().build();
		TxnRequest txnRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(HiveBonusFundDetails.builder().build())
				.build();
		txn.setEvents(new ArrayList<>(List.of(txnRequest)));
		TxnRequest request = BonusWalletMapping.txnToTxnRequest(txn);

		assertThat(request.getTxnId()).isEqualTo(txn.getTxnId());
	}

	@Test
	public void txnWithoutBonusFundDetailsToRequest() {
		Txn txn = TxnBuilder.txn().build();
		assertThatThrownBy(() -> BonusWalletMapping.txnToTxnRequest(txn)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void txnCancelToCancelRequest() {
		TxnCancel cancel = TxnCancelBuilder.txnCancel().build();
		Txn txn = TxnBuilder.txn().build();

		TxnCancelRequest cancelRequest = BonusWalletMapping.txnToTxnCancelRequest(txn, cancel);

		assertThat(cancelRequest.getCancelType()).isEqualTo(TxnCancelType.RECON);

		assertThat(cancelRequest.getPlayComplete()).isEqualTo(cancel.isPlayComplete());
		assertThat(cancelRequest.getRoundComplete()).isEqualTo(cancel.isRoundComplete());

		assertThat(cancelRequest.getTxnId()).isEqualTo(txn.getTxnId());
		assertThat(cancelRequest.getGameCode()).isEqualTo(txn.getGameCode());

		assertThat(cancelRequest.getType()).isEqualTo(EventType.txnCancelRequest);
	}

	@Test
	public void txnWithCancelRequestCancelToCancelRequest() {
		TxnCancel cancel = TxnCancelBuilder.txnCancel().build();

		TxnCancelRequest cancelRequest = TxnCancelRequestBuilder.txnCancelRequest()
				.withCancelType(TxnCancelType.PLAYER)
				.build();

		ArrayList<TxnEvent> requests = new ArrayList<>();
		requests.add(cancelRequest);

		Txn txn = TxnBuilder.txn().withTxnEvents(requests).build();

		TxnCancelRequest convertedCancelRequest = BonusWalletMapping.txnToTxnCancelRequest(txn, cancel);

		assertThat(convertedCancelRequest.getCancelType()).isEqualTo(TxnCancelType.PLAYER);

		assertThat(convertedCancelRequest.getPlayComplete()).isEqualTo(cancel.isPlayComplete());
		assertThat(convertedCancelRequest.getRoundComplete()).isEqualTo(cancel.isRoundComplete());

		assertThat(convertedCancelRequest.getTxnId()).isEqualTo(txn.getTxnId());
		assertThat(convertedCancelRequest.getGameCode()).isEqualTo(txn.getGameCode());

		assertThat(cancelRequest.getType()).isEqualTo(EventType.txnCancelRequest);
	}
}
