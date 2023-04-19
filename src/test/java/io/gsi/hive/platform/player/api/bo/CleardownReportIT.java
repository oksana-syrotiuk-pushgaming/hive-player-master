package io.gsi.hive.platform.player.api.bo;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCleardownBuilder;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.persistence.TxnCleardownRepository;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL})
public class CleardownReportIT extends ApiITBase {

  @Autowired
  protected TxnCleardownRepository txnCleardownRepository;

  @Autowired
  @Qualifier(CacheConfig.CLEARDOWN_REPORT_CACHE_NAME)
  private CaffeineCache cleardownReportCache;

  @Before
  public void setup() {
    cleardownReportCache.clear();
  }

  @Test
  public void givenMultipleIgpCodes_whenGetPlayReport_thenCorrectCleardownReport() {
    playerRepository.saveAll(Arrays.asList(
            PlayerBuilder.aPlayer().build(),
            PlayerBuilder.aPlayer().withPlayerId("player2").withIgpCode("gecko").build(),
            PlayerBuilder.aPlayer().withPlayerId("player3").withIgpCode("gecko").build(),
            PlayerBuilder.aPlayer().withPlayerId("player4").withIgpCode("newt").build(),
            PlayerBuilder.aPlayer().withPlayerId("player5").withIgpCode("newt").build()
    ));

    txnRepository.saveAll(Arrays.asList(
            TxnBuilder.txn().withStatus(TxnStatus.OK).build(),
            TxnBuilder.txn().withTxnId("1000-2").withPlayerId("player2").withIgpCode("gecko").withStatus(TxnStatus.OK).build(),
            TxnBuilder.txn().withTxnId("1000-3").withPlayerId("player3").withIgpCode("gecko").withStatus(TxnStatus.OK).build(),
            TxnBuilder.txn().withTxnId("1000-4").withPlayerId("player4").withIgpCode("newt").withStatus(TxnStatus.OK).build(),
            TxnBuilder.txn().withTxnId("1000-5").withPlayerId("player5").withIgpCode("newt").withStatus(TxnStatus.OK).build()
    ));

    txnCleardownRepository.saveAll(Arrays.asList(
            TxnCleardownBuilder.txn().build(),
            TxnCleardownBuilder.txn().withTxnId("1000-2").withAmount(BigDecimal.ONE).withCleardownTxnId("1000-2").build(),
            TxnCleardownBuilder.txn().withTxnId("1000-3").withAmount(BigDecimal.TEN).withCleardownTxnId("1000-3").build(),
            TxnCleardownBuilder.txn().withTxnId("1000-4").withAmount(BigDecimal.valueOf(100L)).withCleardownTxnId("1000-4").build(),
            TxnCleardownBuilder.txn().withTxnId("1000-5").withAmount(BigDecimal.valueOf(50L)).withCleardownTxnId("1000-5").build()
    ));

    RestAssured.given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .queryParam("igpCodes", "iguana","gecko","newt")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .queryParam("groupBy", "ccy_code")
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .statusCode(200)
        .body("[0].ccyCode", equalTo("GBP"))
        .body("[0].numCleardowns", equalTo(2))
        .body("[0].totalWin", equalTo(11.0f))
        .body("[0].uniquePlayers", equalTo(2))
        .body("[0].igpCode",equalTo("gecko"))
        .body("[1].ccyCode", equalTo("GBP"))
        .body("[1].numCleardowns", equalTo(1))
        .body("[1].totalWin", equalTo(20.0f))
        .body("[1].uniquePlayers", equalTo(1))
        .body("[1].igpCode",equalTo("iguana"))
        .body("[2].ccyCode", equalTo("GBP"))
        .body("[2].numCleardowns", equalTo(2))
        .body("[2].totalWin", equalTo(150.0f))
        .body("[2].uniquePlayers", equalTo(2))
        .body("[2].igpCode",equalTo("newt"));

  }

  @Test
  public void givenSingleIgpCode_whenGetReport_thenCorrectCleardownReport() {
    this.savePlayer(PlayerBuilder.aPlayer().build());
    this.saveTxn(TxnBuilder.txn().withStatus(TxnStatus.OK).build());
    txnCleardownRepository.save(TxnCleardownBuilder.txn().build());

    RestAssured.given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .queryParam("igpCodes", "iguana")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .queryParam("groupBy", "ccy_code")
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .body("[0].ccyCode", equalTo("GBP"))
        .body("[0].numCleardowns", equalTo(1))
        .body("[0].totalWin", equalTo(20.0f))
        .body("[0].uniquePlayers", equalTo(1))
        .body("[0].igpCode",equalTo("iguana"));
  }

  @Test
  public void givenPlayerAndPendingMeshTxns_whenGetReport_thenPendingTxnsFilteredInReport() {
    this.savePlayer(PlayerBuilder.aPlayer().build());

    txnRepository.saveAll(Arrays.asList(
            TxnBuilder.txn().withTxnId("1001-1").withStatus(TxnStatus.OK).build(),
            TxnBuilder.txn().withTxnId("1001-2").withStatus(TxnStatus.PENDING).build()
    ));

    txnCleardownRepository.saveAll(Arrays.asList(
            TxnCleardownBuilder.txn().withTxnId("1001-1").withCleardownTxnId("1001-1").build(),
            TxnCleardownBuilder.txn().withTxnId("1001-2").withCleardownTxnId("1001-2").build()
    ));

    RestAssured.given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .queryParam("igpCodes", "iguana")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .queryParam("groupBy", "ccy_code")
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .body("[0].ccyCode", equalTo("GBP"))
        .body("[0].numCleardowns", equalTo(1))
        .body("[0].totalWin", equalTo(20.0f))
        .body("[0].uniquePlayers", equalTo(1))
        .body("[0].igpCode",equalTo("iguana"));
  }

  @Test
  public void givenFilterByCountry_whenGetReport_thenReportFilteredByCountry() {
    this.savePlayer(PlayerBuilder.aPlayer().withCountry("FR").build());

    txnRepository.saveAll(Arrays.asList(
            TxnBuilder.txn().build(),
            TxnBuilder.txn().withStatus(TxnStatus.OK).build()
    ));

    txnCleardownRepository.save(TxnCleardownBuilder.txn().build());

    RestAssured.given()
        .queryParam("igpCodes", "iguana")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .queryParam("groupBy", "ccy_code", "country")
        .queryParam("country","FR")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .statusCode(200)
        .body("[0].ccyCode", equalTo("GBP"))
        .body("[0].numCleardowns", equalTo(1))
        .body("[0].totalWin", equalTo(20.0f))
        .body("[0].uniquePlayers", equalTo(1))
        .body("[0].country", equalTo("FR"))
        .body("[0].igpCode",equalTo("iguana"));
  }

  @Test
  public void givenFilterByCcyCode_whenGetReport_thenReportFilteredByCcyCode() {
    this.savePlayer(PlayerBuilder.aPlayer().build());

    txnRepository.saveAll(Arrays.asList(
            TxnBuilder.txn().build(),
            TxnBuilder.txn().withTxnId("1").withCcyCode("GBP").withStatus(TxnStatus.OK).build(),
            TxnBuilder.txn().withTxnId("2").withCcyCode("USD").withStatus(TxnStatus.OK).build(),
            TxnBuilder.txn().withTxnId("3").withCcyCode("USD").withStatus(TxnStatus.OK).build()
    ));

    txnCleardownRepository.saveAll(Arrays.asList(
            TxnCleardownBuilder.txn().withTxnId("1").withCleardownTxnId("1").build(),
            TxnCleardownBuilder.txn().withTxnId("2").withCleardownTxnId("2").build(),
            TxnCleardownBuilder.txn().withTxnId("3").withCleardownTxnId("3").build()
    ));

    RestAssured.given()
        .queryParam("igpCodes", "iguana")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .queryParam("groupBy", "ccy_code")
        .queryParam("ccyCode","USD")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .statusCode(200)
        .body("[0].ccyCode", equalTo("USD"))
        .body("[0].numCleardowns", equalTo(2))
        .body("[0].totalWin", equalTo(40.0f))
        .body("[0].uniquePlayers", equalTo(1))
        .body("[0].igpCode",equalTo("iguana"));
  }

  @Test
  public void givenFilterByPlayer_whenFilterByPlayerId_thenReportFilteredByPlayerId() {
    this.savePlayer(PlayerBuilder.aPlayer().withPlayerId("player3").build());

    txnRepository.saveAll(Arrays.asList(
            TxnBuilder.txn().withPlayerId("player3").build(),
            TxnBuilder.txn().withPlayerId("player3").withStatus(TxnStatus.OK).build()
    ));

    txnCleardownRepository.save(TxnCleardownBuilder.txn().build());

    RestAssured.given()
        .queryParam("igpCodes", "iguana")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .queryParam("groupBy", "ccy_code")
        .queryParam("playerId","player3")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .statusCode(200)
        .body("[0].ccyCode", equalTo("GBP"))
        .body("[0].numCleardowns", equalTo(1))
        .body("[0].totalWin", equalTo(20.0f))
        .body("[0].uniquePlayers", equalTo(1))
        .body("[0].igpCode",equalTo("iguana"));
  }

  @Test
  public void givenNoGroupyBy_whenGetReport_thenThrowsError() {
    RestAssured.given()
        .queryParam("igpCodes", "iguana")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .statusCode(400);
  }

  @Test
  public void givenInvalidDate_whenGetReport_thenThrowsError() {
    RestAssured.given()
        .queryParam("igpCodes", "iguana")
        .queryParam("dateFrom","This is not a date")
        .queryParam("dateTo", "This is not a date")
        .queryParam("groupBy", "ccy_code")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .statusCode(400);
  }

  @Test
  public void givenException_whenCleardownReport_thenInternalServerExceptionThrown() {
    doThrow(new RuntimeException("test"))
        .when(namedParameterJdbcTemplate)
        .query(any(), any(SqlParameterSource.class), any(RowMapper.class));
    RestAssured.given()
        .queryParam("igpCodes", "gecko")
        .queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
        .queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
        .queryParam("groupBy", "ccy_code")
        .queryParam("playerId","player3")
        .log().all()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/hive/bo/platform/player/v1/cleardown/report")
        .then()
        .log().all()
        .statusCode(500)
        .body("msg",equalTo("Cleardown report failed."));
  }
}
