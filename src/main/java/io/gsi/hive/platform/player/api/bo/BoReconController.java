package io.gsi.hive.platform.player.api.bo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.recon.ManualReconService;
import io.gsi.hive.platform.player.txn.TxnStatus;

@RestController
@RequestMapping("/bo/platform/player/v1/txn")
@Loggable
public class BoReconController {

	private final ManualReconService manualReconService;

	public BoReconController(ManualReconService manualReconService) {
		this.manualReconService = manualReconService;
	}

	/**
	 * @param txnId
	 * @return New Txn Status
	 */
	@PostMapping(path="/{txnId}/recon/requeue")
	public TxnStatus requeueReconTxn(@PathVariable("txnId") String txnId) {

		return manualReconService.requeueReconTxn(txnId);
	}

	@PatchMapping(path="/{txnId}/recon/status")
	public TxnStatus updateTxnStatus(@PathVariable("txnId") String txnId,
			@RequestParam(value = "txnStatus", required=true) TxnStatus txnStatus) {

		if(txnStatus != TxnStatus.OK && txnStatus != TxnStatus.CANCELLED) {
			throw new InvalidStateException("Txn status can only be updated to OK or CANCELLED");
		}

		return manualReconService.updateTxnStatus(txnId, txnStatus);
	}
}
