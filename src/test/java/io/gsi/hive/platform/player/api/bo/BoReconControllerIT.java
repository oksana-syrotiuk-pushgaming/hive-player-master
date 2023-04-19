package io.gsi.hive.platform.player.api.bo;

import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.api.s2s.HivePlayerAuthInterceptor;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.recon.ManualReconService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/*Manual Txn Service is IT's separately, so this just covers the API and Exceptions*/
public class BoReconControllerIT extends ApiITBase{

	@MockBean
	private ManualReconService manualReconService;
	
	@Test
	public void okRequeueReconTxn()
	{
		Mockito.when(manualReconService.requeueReconTxn(Mockito.anyString())).thenReturn(TxnStatus.RECON);
		
		RestAssured.given()
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.pathParam("txnId", TxnPresets.TXNID)
		.post("/hive/bo/platform/player/v1/txn/{txnId}/recon/requeue")
		.then()
		.log().all()
		.statusCode(200)
		.body(equalTo("\"RECON\""));

		Mockito.verify(manualReconService).requeueReconTxn(TxnPresets.TXNID);
	}
	
	@Test
	public void failRequeueOkTxn()
	{
		Mockito.when(manualReconService.requeueReconTxn(Mockito.anyString())).thenThrow(new InvalidStateException(""));
		
		RestAssured.given()
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.pathParam("txnId", TxnPresets.TXNID)
		.post("/hive/bo/platform/player/v1/txn/{txnId}/recon/requeue")
		.then()
		.log().all()
		.statusCode(409)
		.body("code", equalTo("InvalidState"));

		Mockito.verify(manualReconService).requeueReconTxn(TxnPresets.TXNID);
	}
	
	@Test
	public void updateStatusToOK()
	{
		Mockito.when(manualReconService.updateTxnStatus(Mockito.anyString(), Mockito.any())).thenReturn(TxnStatus.OK);
		
		RestAssured.given()
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.pathParam("txnId", TxnPresets.TXNID)
		.queryParam("txnStatus", TxnStatus.OK.name())
		.patch("/hive/bo/platform/player/v1/txn/{txnId}/recon/status")
		.then()
		.log().all()
		.statusCode(200)
		.body(equalTo("\"OK\""));

		Mockito.verify(manualReconService).updateTxnStatus(TxnPresets.TXNID, TxnStatus.OK);
	}
	
	@Test
	public void failUpdateToInvalidStatus()
	{
		Mockito.when(manualReconService.updateTxnStatus(Mockito.anyString(), Mockito.any())).thenThrow(new InvalidStateException(""));
		
		RestAssured.given()
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.pathParam("txnId", TxnPresets.TXNID)
		.queryParam("txnStatus", TxnStatus.FAILED.name())
		.patch("/hive/bo/platform/player/v1/txn/{txnId}/recon/status")
		.then()
		.log().all()
		.statusCode(409)
		.body("code", equalTo("InvalidState"));
		
		Mockito.verify(manualReconService, Mockito.times(0)).updateTxnStatus(Mockito.eq(TxnPresets.TXNID), Mockito.any());
	}
}
