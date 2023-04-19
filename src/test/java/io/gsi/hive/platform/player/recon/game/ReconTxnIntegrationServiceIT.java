package io.gsi.hive.platform.player.recon.game;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.game.Game;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ReconTxnIntegrationServiceIT extends ApiITBase{

	@Autowired
	private ReconTxnIntegrationService reconIntegrationService;
	
	@MockBean private TxnService txnService;
	@MockBean private TxnRepository txnRepository;
	@MockBean private GameService gameService;
	@MockBean private TxnCallbackRepository txnCallbackRepository;
		
	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
		//We're only interested in the Id of the game from the lookup
		Game gameMock = Mockito.mock(Game.class);
		
		Mockito.when(gameService.getGame(GamePresets.CODE)).thenReturn(gameMock);
	}
	
	@Test
	public void okTxnNotStoredAddedToRecon() {
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
			
		reconIntegrationService.integrateGameTxn(gameTxn);
		
		//Should look in repo, then add as regular txn when not found
		Mockito.verify(txnRepository).findAndLockByTxnId(TxnPresets.TXNID);
		Mockito.verify(txnService).create(gameTxn);	
	}
	
	@Test
	public void okTxnStoredFailedCallbackAdded() {
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
			
		Mockito.when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(TxnBuilder.txn().withStatus(TxnStatus.FAILED).build());
		
		reconIntegrationService.integrateGameTxn(gameTxn);
		
		//Should look in repo, then perform callback when stored is failed
		Mockito.verify(txnRepository).findAndLockByTxnId(TxnPresets.TXNID);
		Mockito.verify(txnService, Mockito.times(0)).create(gameTxn);	
		
		Mockito.verify(txnCallbackRepository).saveToCallbackQueue(gameTxn.getTxnId(), GamePresets.CODE, TxnStatus.FAILED.name());
	}
	
	@Test
	public void okTxnStoredPendingIgnored() {
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
			
		Mockito.when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(TxnBuilder.txn().build());
		
		reconIntegrationService.integrateGameTxn(gameTxn);
		
		//Should look in repo, then ignore because its pending there
		Mockito.verify(txnRepository).findAndLockByTxnId(TxnPresets.TXNID);
		Mockito.verify(txnService, Mockito.times(0)).create(gameTxn);	
		
		//Callback should not be made
		Mockito.verify(txnCallbackRepository, Mockito.times(0)).saveToCallbackQueue(Mockito.anyString(), Mockito.anyString(), Mockito.any());

	}
	
	@Test
	public void okTxnStoredStakeOkCancelled() {
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
			
		Txn storedTxn = TxnBuilder.txn().withStatus(TxnStatus.OK).build();
		Mockito.when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(storedTxn);
		
		reconIntegrationService.integrateGameTxn(gameTxn);
		
		//Should look in repo, then cancel because stake ok there
		Mockito.verify(txnRepository).findAndLockByTxnId(TxnPresets.TXNID);
		Mockito.verify(txnService, Mockito.times(0)).create(gameTxn);	
		
		Mockito.verify(txnRepository).save(storedTxn);
		
		//Callback should not be made
		Mockito.verify(txnCallbackRepository, Mockito.times(0)).saveToCallbackQueue(Mockito.anyString(), Mockito.anyString(), Mockito.any());
	}
	
	//Cancel a txn stored as OK - should be added as cancel for recon
	@Test
	public void okCancelValid()
	{
		TxnCancelRequest cancelRequest = TxnCancelRequestBuilder.txnCancelRequest().build();
		
		//Stored Txn is completed and OK
		Txn storedTxn = TxnBuilder.txn().withStatus(TxnStatus.OK).build();
		Mockito.when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(storedTxn);
		
		reconIntegrationService.integrateCancelTxn(cancelRequest);
		
		Mockito.verify(txnRepository).findAndLockByTxnId(TxnPresets.TXNID);
		
		//Should now be stored as cancelling
		assertThat(storedTxn.getStatus()).isEqualTo(TxnStatus.CANCELLING);
		Mockito.verify(txnRepository).save(storedTxn);
	}
	
	//Cancel a txn we dont have stored - calls back not found
	@Test
	public void okCancelNotFound()
	{
		TxnCancelRequest cancelRequest = TxnCancelRequestBuilder.txnCancelRequest().build();
		
		reconIntegrationService.integrateCancelTxn(cancelRequest);
		
		//Should look in repo then save CB as notfound
		Mockito.verify(txnRepository).findAndLockByTxnId(TxnPresets.TXNID);
		Mockito.verify(txnCallbackRepository).saveToCallbackQueue(cancelRequest.getTxnId(), GamePresets.CODE, TxnStatus.NOTFOUND.name());
	}
	
	//Try to cancel a PENDING txn - not allowed
	@Test
	public void okCancelPendingCallsback()
	{
		TxnCancelRequest cancelRequest = TxnCancelRequestBuilder.txnCancelRequest().build();
		
		//Stored Txn is PENDING
		Txn storedTxn = TxnBuilder.txn().build();
		Mockito.when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(storedTxn);
		
		reconIntegrationService.integrateCancelTxn(cancelRequest);
		
		Mockito.verify(txnRepository).findAndLockByTxnId(TxnPresets.TXNID);
		
		Mockito.verify(txnCallbackRepository).saveToCallbackQueue(cancelRequest.getTxnId(), storedTxn.getGameCode(), storedTxn.getStatus().name());
	}
}
