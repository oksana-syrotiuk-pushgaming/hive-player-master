package io.gsi.hive.platform.player.platformidentifier;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import org.junit.Before;
import org.junit.Test;

public class PlatformIdentifierTests {

  private PlatformIdentifierService platformIdentifierService;
  private TxnRequest newFormatTxnRequest;
  private TxnRequest oldFormatTxnRequest;
  private TxnRequest pgSlotOldFormatTxnRequest;
  private TxnRequest pgSlotNewFormatTxnRequest;

  @Before
  public void setup(){
    platformIdentifierService = new PlatformIdentifierService(
        "9999", false);
    //GSI Slot format
    newFormatTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("1000-9999-12345")
        .playId("1000-9999-54321")
        .roundId("1000-9999-54321").build();
    oldFormatTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("1000-12345")
        .playId("1000-54321")
        .roundId("1000-54321").build();

    //PG Slot format
    pgSlotOldFormatTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("11000-12345-12345")
        .playId("11000-54321")
        .roundId("11000-54321").build();
    pgSlotNewFormatTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("11000-9999-12345-12345")
        .playId("11000-9999-54321")
        .roundId("11000-9999-54321").build();
  }

  @Test
  public void givenOldAndNewTxnRequests_whenStrictValidationDisabled_thenOK(){
    platformIdentifierService.validateTxnRequestPrefixes(newFormatTxnRequest);
    platformIdentifierService.validateTxnRequestPrefixes(oldFormatTxnRequest);
    platformIdentifierService.validateTxnRequestPrefixes(pgSlotOldFormatTxnRequest);
    platformIdentifierService.validateTxnRequestPrefixes(pgSlotNewFormatTxnRequest);
  }

  @Test
  public void givenNewTxnRequests_whenStrictValidationEnable_thenOk(){
    platformIdentifierService = new PlatformIdentifierService(
        TxnPresets.PLATFORMID, true);
     platformIdentifierService.validateTxnRequestPrefixes(newFormatTxnRequest);
     platformIdentifierService.validateTxnRequestPrefixes(pgSlotNewFormatTxnRequest);
  }


  @Test
  public void givenOldTxnRequests_whenStrictValidationEnabled_thenThrowException() {
    platformIdentifierService = new PlatformIdentifierService(
        "9999", true);

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(oldFormatTxnRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("missing platform instance identifier");
    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(pgSlotOldFormatTxnRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("missing platform instance identifier");
  }


  @Test
  public void givenGSISlotRequestWithIncorrectPlatformId_whenValidating_thenThrowsException(){
    TxnRequest invalidTxnId = defaultStakeTxnRequestBuilder()
        .txnId("1000-9998-12345")//incorrect value
        .playId("1000-9999-54321")
        .roundId("1000-9999-54321").build();
    TxnRequest invalidPlayId = defaultStakeTxnRequestBuilder()
        .txnId("1000-9999-12345")
        .playId("1000-9998-54321")//incorrect value.
        .roundId("1000-9999-54321").build();
    TxnRequest invalidRoundId = defaultStakeTxnRequestBuilder()
        .txnId("1000-9999-12345")
        .playId("1000-9999-54321")
        .roundId("1000-9998-54321").build();//incorrect value

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(invalidTxnId);
    }).isInstanceOf(BadRequestException.class)
    .hasMessage("txnId platform identifier does not match injected value.");

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(invalidPlayId);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("playId platform identifier does not match injected value.");

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(invalidRoundId);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("roundId platform identifier does not match injected value.");
  }

  @Test
  public void givenPGSlotRequestWithIncorrectPlatformId_whenValidating_thenThrowsException(){
    TxnRequest invalidTxnId = defaultStakeTxnRequestBuilder()
        .txnId("11000-9998-12345-12345")//incorrect value
        .playId("11000-9999-12345")
        .roundId("11000-9999-12345").build();
    TxnRequest invalidPlayId = defaultStakeTxnRequestBuilder()
        .txnId("11000-9999-12345-12345")
        .playId("11000-9998-12345")//incorrect value.
        .roundId("11000-9999-12345").build();
    TxnRequest invalidRoundId = defaultStakeTxnRequestBuilder()
        .txnId("11000-9999-12345-12345")
        .playId("11000-9999-12345")
        .roundId("11000-9998-12345").build();//incorrect value

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(invalidTxnId);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("txnId platform identifier does not match injected value.");

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(invalidPlayId);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("playId platform identifier does not match injected value.");

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(invalidRoundId);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("roundId platform identifier does not match injected value.");
  }

  @Test
  public void givenGSISlotRequestWithInternalIds_whenValidating_thenThrowException() {
    TxnRequest internalPlayIdTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("1000-9999-12345")
        .playId("54321")//incorrect value.
        .roundId("1000-9999-54321").build();
    TxnRequest internalRoundIdTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("1000-9999-12345")
        .playId("1000-9999-54321")
        .roundId("54321").build();//incorrect value.

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(internalPlayIdTxnRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unknown gsi-slot id format! playId");

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(internalRoundIdTxnRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unknown gsi-slot id format! roundId");
  }

  @Test
  public void givenPGSlotRequestWithInternalIds_whenValidating_thenThrowException() {
    TxnRequest internalTxnIdTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("12345-12345")//incorrect value.
        .playId("11000-9998-54321")
        .roundId("11000-9999-54321").build();
    TxnRequest internalPlayIdTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("11000-9999-12345-12345")
        .playId("54321")//incorrect value.
        .roundId("11000-9999-54321").build();
    TxnRequest internalRoundIdTxnRequest = defaultStakeTxnRequestBuilder()
        .txnId("11000-9999-12345-12345")
        .playId("11000-9999-54321")
        .roundId("54321").build();//incorrect value.

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(internalTxnIdTxnRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unknown pg-slot id format! txnId");

    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(internalPlayIdTxnRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unknown pg-slot id format! playId");
    assertThatThrownBy(() -> {
      platformIdentifierService.validateTxnRequestPrefixes(internalRoundIdTxnRequest);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unknown pg-slot id format! roundId");
  }
}
