package io.gsi.hive.platform.player.recon;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.autocompletion.AutocompleteRequest;
import io.gsi.hive.platform.player.builders.AutocompleteRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnAuditRepository;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.registry.RegistryGateway;
import io.gsi.hive.platform.player.registry.txn.IGPCodes;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnAuditAction;
import io.gsi.hive.platform.player.txn.TxnCallback;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.BonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnEvent;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Sql(statements={PersistenceITBase.CLEAN_DB_SQL,PersistenceITBase.PLAYER_SQL}, executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements={PersistenceITBase.CLEAN_DB_SQL}, executionPhase= Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class ManualReconServiceIT extends ApiITBase {

	@Autowired private ManualReconService manualReconService;
	@Autowired private TxnRepository txnRepository;
	@Autowired private TxnCallbackRepository callbackRepository;
	@Autowired private TxnAuditRepository txnAuditRepository;
	@Autowired private PlayRepository playRepository;
	@Autowired private AutocompleteRequestRepository autocompleteRequestRepository;

	@MockBean private RegistryGateway registryGateway;

	@Test
	public void givenStakeTxnInReconAndCanceledStatus_whenUpdateTxnStatus_thenTxnStatusSetToCancelled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn stakeTxn = TxnBuilder.txn()
				.withStatus(TxnStatus.RECON)
				.build();

		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		setReconStakeTxnStatusToCancelled(stakeTxn);
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play removed from autocomplete request queue")
				.isEmpty();
	}

	@Test
	public void givenOpFreeRoundsStakeTxnInReconAndCanceledStatus_whenUpdateTxnStatus_thenTxnStatusSetToCancelled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn operatorFreeroundsTxn = TxnBuilder.txn()
				.withStatus(TxnStatus.RECON)
				.withType(TxnType.OPFRSTK)
				.build();

		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		setReconStakeTxnStatusToCancelled(operatorFreeroundsTxn);
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play removed from autocomplete request queue")
				.isEmpty();
	}

	private void setReconStakeTxnStatusToCancelled(Txn txn) {
		txnRepository.saveAndFlush(txn);
		var play = PlayBuilder.play()
				.withPlayId(txn.getPlayId())
				.withStatus(PlayStatus.ACTIVE)
				.build();
		playRepository.save(play);


		assertThat(txn.getStatus()).isNotEqualTo(TxnStatus.CANCELLED);

		manualReconService.updateTxnStatus(txn.getTxnId(), TxnStatus.CANCELLED);

		assertThat(txnRepository.findById(txn.getTxnId()).get().getStatus())
				.isEqualTo(TxnStatus.CANCELLED);
		assertThat(callbackRepository.findById(txn.getTxnId()))
				.contains(new TxnCallback(txn.getTxnId(), txn.getGameCode(), TxnStatus.CANCELLED));
		assertThat(txnAuditRepository.findById(txn.getTxnId()).get().getAction())
				.isEqualTo(TxnAuditAction.FORCE_CANCELLED);
	}

	@Test
	public void givenWinTxnInReconAndOkStatus_whenUpdateTxnStatus_thenTxnStatusSetToOk() {
		Play play = PlayBuilder.play()
				.withBonusFundType(OperatorBonusFundDetails.TYPE)
				.withStatus(PlayStatus.ACTIVE)
				.build();
		playRepository.saveAndFlush(play);
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn winTxn = TxnBuilder.txn()
				.withType(TxnType.WIN)
				.withStatus(TxnStatus.RECON)
				.build();

		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		setReconWinTxnStatusToOk(winTxn);
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play removed from autocomplete request queue")
				.isEmpty();
	}

	@Test
	public void givenOpFreeRoundsWinTxnInReconAndOkStatus_whenUpdateTxnStatus_thenTxnStatusSetToOk() {
		Play play = PlayBuilder.play()
				.withBonusFundType(OperatorBonusFundDetails.TYPE)
				.withStatus(PlayStatus.ACTIVE)
				.build();
		playRepository.saveAndFlush(play);
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn operatorFreeroundsWinTxn = TxnBuilder.txn()
				.withType(TxnType.WIN)
				.withStatus(TxnStatus.RECON)
				.build();

		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		setReconWinTxnStatusToOk(operatorFreeroundsWinTxn);
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play removed from autocomplete request queue")
				.isEmpty();
	}

	private void setReconWinTxnStatusToOk(Txn txn) {
		txnRepository.saveAndFlush(txn);

		assertThat(txn.getStatus()).isNotEqualTo(TxnStatus.OK);

		manualReconService.updateTxnStatus(txn.getTxnId(), TxnStatus.OK);

		assertThat(txnRepository.findById(txn.getTxnId()).get().getStatus())
				.isEqualTo(TxnStatus.OK);
		assertThat(callbackRepository.findById(txn.getTxnId()))
				.contains(new TxnCallback(txn.getTxnId(), txn.getGameCode(), TxnStatus.OK));
		assertThat(txnAuditRepository.findById(txn.getTxnId()).get().getAction())
				.isEqualTo(TxnAuditAction.FORCE_OK);
	}

	@Test
	public void givenTxnNotInReconStatusAndOkStatus_whenUpdateTxnStatus_thenInvalidStateExceptionThrown() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn txn = TxnBuilder.txn().build();
		txnRepository.saveAndFlush(txn);

		var txnId = txn.getTxnId();
		assertThat(txn.getStatus()).isNotEqualTo(TxnStatus.RECON);
		assertThatThrownBy(() -> manualReconService.updateTxnStatus(txnId, TxnStatus.OK))
				.isInstanceOf(InvalidStateException.class);

		txn.setBalance(new BigDecimal("1000.00"));
		assertThat(txnRepository.findById(txnId)).contains(txn);
		assertThat(callbackRepository.findById(txnId)).isEmpty();
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play not removed from autocomplete request queue")
				.contains(autocompleteRequest);

	}

	@Test
	public void givenStakeTxnInReconAndOkStatus_whenUpdateTxnStatus_thenInvalidStateExceptionThrown() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn txn = TxnBuilder.txn().withStatus(TxnStatus.RECON).build();
		txnRepository.saveAndFlush(txn);

		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		var txnId = txn.getTxnId();
		assertThat(txn.getStatus()).isNotEqualTo(TxnStatus.OK);
		assertThatThrownBy(() -> manualReconService.updateTxnStatus(txnId, TxnStatus.OK))
				.isInstanceOf(InvalidStateException.class);
		assertThat(callbackRepository.findById(txn.getTxnId())).isEmpty();
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play not removed from autocomplete request queue")
				.contains(autocompleteRequest);
	}

	@Test
	public void givenWinTxnInReconAndCanceledStatus_whenUpdateTxnStatus_thenInvalidStateExceptionThrown() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn txn = TxnBuilder.txn().withType(TxnType.WIN).withStatus(TxnStatus.RECON).build();
		txnRepository.saveAndFlush(txn);

		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		var txnId = txn.getTxnId();
		assertThatThrownBy(() -> manualReconService.updateTxnStatus(txnId, TxnStatus.CANCELLED))
				.isInstanceOf(InvalidStateException.class);

		txn.setBalance(new BigDecimal("1000.00"));
		assertThat(txnRepository.findById(txnId)).contains(txn);
		assertThat(callbackRepository.findById(txnId)).isEmpty();
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play not removed from autocomplete request queue")
				.contains(autocompleteRequest);
	}

	@DisplayName("Forcing the status of a txn should give error when the IGP code is not allowed")
	@Test
	public void givenStakeTxnInReconAndCanceledStatus_whenUpdateTxnStatusAndIgpCodeInRegistry_thenForbiddenExceptionThrown() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		Txn stakeTxn = TxnBuilder.txn()
				.withStatus(TxnStatus.RECON)
				.build();

		when(registryGateway.getConfigIgpCodes()).thenReturn(IGPCodes.builder().igpCodesList(List.of(IgpPresets.IGPCODE_IGUANA)).build());

		assertThatThrownBy(() -> setReconStakeTxnStatusToCancelled(stakeTxn))
				.isInstanceOf(BadRequestException.class)
				.isEqualToComparingFieldByField(new BadRequestException(""))
				.hasMessage("Txn "+stakeTxn.getTxnId()+" belongs to IGP Code "+stakeTxn.getIgpCode()+" for which status forcing is disabled");
	}

	@Test
	public void givenStakeTxnInReconAndNoRetry_whenRequeueReconTxn_thenRetriesResetAndStatusSetToPending() {
		Txn txn = TxnBuilder.txn().withStatus(TxnStatus.RECON).withRetry(10).build();
		txnRepository.saveAndFlush(txn);

		manualReconService.requeueReconTxn(txn.getTxnId());

		Txn foundTxn = txnRepository.findById(txn.getTxnId()).get();
		assertThat(foundTxn.getStatus()).isEqualTo(TxnStatus.PENDING);
		assertThat(foundTxn.getRetry()).isZero();

		var before = ZonedDateTime.now().minusDays(5);
		var queuedTxns  = txnRepository.findReconTxns(before, 100);
		assertThat(queuedTxns).contains(txn.getTxnId());
	}

	@Test
	public void givenStakeTxnNotInReconAndNoRetry_whenRequeueReconTxn_thenInvalidStateExceptionThrown() {
		Txn txn = TxnBuilder.txn().build();
		txnRepository.saveAndFlush(txn);

		assertThat(txn.getStatus()).isEqualTo(TxnStatus.PENDING);

		var txnId = txn.getTxnId();
		assertThatThrownBy(() -> manualReconService.requeueReconTxn(txnId))
				.isInstanceOf(InvalidStateException.class);
	}

	@Test
	public void givenUnrecognisedTxnIdAndNoRetry_whenRequeueReconTxn_thenInvalidStateExceptionThrown() {
		assertThatThrownBy(() -> manualReconService.requeueReconTxn("NotARealID"))
				.isInstanceOf(InvalidStateException.class);
	}

	@Test
	public void givenStakeTxnInReconAndWithRetry_whenRequeueReconTxn_thenRetriesResetAndStatusSetToPending() {
		Txn txn = TxnBuilder.txn().withStatus(TxnStatus.RECON).withRetry(10).build();
		txnRepository.saveAndFlush(txn);

		manualReconService.requeueReconTxn(txn.getTxnId(), 5);

		Txn foundTxn = txnRepository.findById(txn.getTxnId()).get();
		assertThat(foundTxn.getStatus()).isEqualTo(TxnStatus.PENDING);
		assertThat(foundTxn.getRetry()).isEqualTo(5);

		var before = ZonedDateTime.now().minusDays(5);
		var queuedTxns  = txnRepository.findReconTxns(before, 100);
		assertThat(queuedTxns).contains(txn.getTxnId());
	}

	public void givenNullRetryNumber_whenRequeueReconTxn_thenBadRequestExceptionThrown(){
		Txn txn = TxnBuilder.txn().withStatus(TxnStatus.RECON).withRetry(10).build();
		txnRepository.saveAndFlush(txn);

		manualReconService.requeueReconTxn(txn.getTxnId(), null);

		Txn foundTxn = txnRepository.findById(txn.getTxnId()).get();
		assertThat(foundTxn.getStatus()).isEqualTo(TxnStatus.PENDING);
		assertThat(foundTxn.getRetry()).isEqualTo(10);

		var before = ZonedDateTime.now().minusDays(5);
		var queuedTxns  = txnRepository.findReconTxns(before, 100);
		assertThat(queuedTxns).contains(txn.getTxnId());
	}

	@Test
	public void givenStakeTxnNotInReconAndWithRetry_whenRequeueReconTxn_thenInvalidStateExceptionThrown() {
		Txn txn = TxnBuilder.txn().build();
		txnRepository.saveAndFlush(txn);

		assertThat(txn.getStatus()).isEqualTo(TxnStatus.PENDING);

		var txnId = txn.getTxnId();
		assertThatThrownBy(() -> manualReconService.requeueReconTxn(txnId, 5))
				.isInstanceOf(InvalidStateException.class);
	}

		@Test
	public void givenUnrecognisedTxnIdAndWithRetry_whenRequeueReconTxn_thenInvalidStateExceptionThrown() {
		assertThatThrownBy(() -> manualReconService.requeueReconTxn("NotARealID", 5))
				.isInstanceOf(InvalidStateException.class);
	}

	@Test
	public void givenNegativeRetryNumber_whenRequeueReconTxn_thenBadRequestExceptionThrown() {
		assertThatThrownBy(() -> manualReconService.requeueReconTxn("ID", -1))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	public void givenRetryNumberOverMaxRetries_whenRequeueReconTxny_thenBadRequestExceptionThrown() {
		assertThatThrownBy(() -> manualReconService.requeueReconTxn("ID", 11))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	public void givenStakeTxnInRecon_whenUpdateTxnStatus_thenPlayStatusSetToCancelled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		var txn = TxnBuilder.txn()
				.withStatus(TxnStatus.RECON)
				.withType(TxnType.STAKE).build();
		txnRepository.saveAndFlush(txn);

		var play = PlayBuilder.play()
				.withPlayId(txn.getPlayId())
				.withStatus(PlayStatus.ACTIVE)
				.build();
		playRepository.save(play);

		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		manualReconService.updateTxnStatus(txn.getTxnId(), TxnStatus.CANCELLED);
		var updatedPlay = playRepository.findById(txn.getPlayId()).get();

		assertThat(updatedPlay.getStatus()).isEqualTo(PlayStatus.VOIDED);
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play removed from autocomplete request queue")
				.isEmpty();
	}

	@Test
	public void givenBonusWinTxnInRecon_whenUpdateTxnStatus_thenBadRequestThrown() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		BonusFundDetails operatorBonusFundDetails = new OperatorBonusFundDetails();
		TxnEvent txnEvent = TxnRequest.builder().bonusFundDetails(operatorBonusFundDetails).build();

		var txn = TxnBuilder.txn()
				.withStatus(TxnStatus.RECON)
				.withTxnEvents(newArrayList(txnEvent))
				.withType(TxnType.WIN).build();
		txnRepository.saveAndFlush(txn);

		var play = PlayBuilder.play()
				.withPlayId(txn.getPlayId())
				.withStatus(PlayStatus.ACTIVE)
				.build();
		playRepository.save(play);

		assertThatThrownBy(() -> manualReconService.updateTxnStatus(txn.getTxnId(), TxnStatus.OK)).isInstanceOf(BadRequestException.class);

		var updatedPlay = playRepository.findById(txn.getPlayId()).get();

		assertThat(updatedPlay.getStatus()).isEqualTo(PlayStatus.ACTIVE);
		assertThat(txn.getStatus()).isEqualTo(TxnStatus.RECON);
	}

	@Test
	public void givenUnrecognisedTxnId_whenUpdateTxnStatus_thenInvalidStateExceptionThrown() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		assertThatThrownBy(() -> manualReconService.updateTxnStatus("AlsoNotARealID", TxnStatus.PENDING))
				.isInstanceOf(InvalidStateException.class);
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play not removed from autocomplete request queue")
				.contains(autocompleteRequest);
	}

	@Test
	public void givenStakeTxnAlreadyCancelling_whenUpdateTxnStatus_thenThrowInvalidStateTxn() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		var txn = TxnBuilder.txn()
				.withStatus(TxnStatus.CANCELLING)
				.withType(TxnType.STAKE).build();
		txnRepository.saveAndFlush(txn);

		var txnId = txn.getTxnId();
		assertThatThrownBy(() -> manualReconService.updateTxnStatus(txnId, TxnStatus.CANCELLING))
				.isInstanceOf(InvalidStateException.class);
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play not removed from autocomplete request queue")
				.contains(autocompleteRequest);
	}

	@Test
	public void givenNoPlayInAutocompletionQueue_whenUpdateTxnStatus_thenNoExceptionThrown() {
		when(registryGateway.getConfigIgpCodes()).thenReturn(new IGPCodes());

		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		assertThat(autocompleteRequestRepository.findById(autocompleteRequest.getPlayId()))
				.as("txn's play removed from autocomplete request queue")
				.isEmpty();

		Txn stakeTxn = TxnBuilder.txn()
				.withStatus(TxnStatus.RECON)
				.build();
		setReconStakeTxnStatusToCancelled(stakeTxn);
	}
}