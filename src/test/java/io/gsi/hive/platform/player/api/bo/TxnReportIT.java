package io.gsi.hive.platform.player.api.bo;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.PlayerTxnBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

import java.math.BigDecimal;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL})
public class TxnReportIT extends ApiITBase
{

	@Autowired @Qualifier(CacheConfig.TXN_REPORT_CACHE_NAME)
	private CaffeineCache txnReportCache;

	@Autowired
	protected PlayRepository playRepository;

	@SpyBean
	@Autowired @Qualifier("reportNamedParameterJdbcTemplate")
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Before
	public void initialiseTestDefaults()
	{
		this.defaultZonedDateTime = TimePresets.ZONEDEPOCHUTC;
		this.defaultDateFrom = TimePresets.ZONEDEPOCHUTC.toString();
		this.defaultDateTo = TimePresets.ZONEDEPOCHUTC.plusMinutes(99).toString();
	}

	@Before
	public void saveTestData() {

		this.savePlayer(
				PlayerBuilder.aPlayer().withCountry("FR")
				.withCcyCode("EUR").withPlayerId("player1").build(),
				PlayerBuilder.aPlayer().withCountry("ES")
				.withCcyCode("EUR").withPlayerId("player2").build(),
				PlayerBuilder.aPlayer().withCountry("GB")
				.withCcyCode("GBP").withPlayerId("player3").build(),
				PlayerBuilder.aPlayer().withCountry("CH")
				.withCcyCode("mBTC").withPlayerId("player4").build(),
				PlayerBuilder.aPlayer().withCountry("GB")
						.withCcyCode("GBP").withPlayerId("player5").withIgpCode("gecko").build(),
				PlayerBuilder.aPlayer().withCountry("CH")
						.withCcyCode("mBTC").withPlayerId("player6").withIgpCode("newt").build());
		this.saveTxn(
				PlayerTxnBuilder.txn().withPlayerId("player1").withCcyCode("EUR").withAmount(BigDecimal.TEN)
						.withType(TxnType.STAKE).withTxnId("txnId1").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayerTxnBuilder.txn().withPlayerId("player1").withCcyCode("EUR").withAmount(BigDecimal.ONE)
						.withType(TxnType.WIN).withTxnId("txnId2").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(2)).build(),
				PlayerTxnBuilder.txn().withPlayerId("player2").withCcyCode("EUR").withAmount(BigDecimal.TEN)
						.withType(TxnType.STAKE).withTxnId("txnId3").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(3)).build(),
				PlayerTxnBuilder.txn().withPlayerId("player3").withCcyCode("GBP").withAmount(BigDecimal.TEN)
						.withType(TxnType.STAKE).withTxnId("txnId4").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(4)).build(),
				PlayerTxnBuilder.txn().withPlayerId("player4").withCcyCode("mBTC").withAmount(BigDecimal.TEN)
						.withType(TxnType.STAKE).withTxnId("txnId5").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(4)).build(),
				PlayerTxnBuilder.txn().withPlayerId("player5").withCcyCode("mBTC").withAmount(BigDecimal.TEN)
						.withType(TxnType.STAKE).withTxnId("txnId6").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(5)).withIgpCode("gecko").build(),
				PlayerTxnBuilder.txn().withPlayerId("player6").withCcyCode("mBTC").withAmount(BigDecimal.TEN)
						.withType(TxnType.STAKE).withTxnId("txnId7").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(5)).withIgpCode("newt").build()
		);

		txnReportCache.clear();
	}

	@Test
	public void okGroupByCcyCode()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("EUR"))
		.body("[0].totalStake", equalTo(20.00f))
		.body("[0].totalWin", equalTo(1.00f))
		.body("[0].bonusCost", equalTo(0.00f))
		.body("[0].numFreeRounds", equalTo(0))
		.body("[0].uniquePlayers", equalTo(2))
		.body("[1].ccyCode", equalTo("GBP"))
		.body("[1].totalStake", equalTo(10.00f))
		.body("[1].totalWin", equalTo(0.00f))
		.body("[1].uniquePlayers", equalTo(1))
		.body("[1].bonusCost", equalTo(0.00f))
		.body("[1].numFreeRounds", equalTo(0))
		.body("[2].ccyCode", equalTo("mBTC"))
		.body("[2].totalStake", equalTo(10.00f))
		.body("[2].totalWin", equalTo(0.00f))
		.body("[2].uniquePlayers", equalTo(1))
		.body("[2].bonusCost", equalTo(0.00f))
		.body("[2].numFreeRounds", equalTo(0));
	}

	@Test
	public void failGroupBySqlInjection()
	{
		RestAssured.given()
				.pathParam("igpCode", "iguana")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code, 1; \n drop table t_txn; --")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/igp/{igpCode}/txn/report")
				.then()
				.log().all()
				.statusCode(400);
	}

	@Test
	public void givenMultipleIgpCodes_whenTxnReport_returnsCorrectResults()
	{
		RestAssured.given()
				.queryParam("igpCodes", "iguana,gecko,newt")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/txn/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].igpCode", equalTo("gecko"))
				.body("[0].ccyCode", equalTo("mBTC"))
				.body("[1].igpCode", equalTo("iguana"))
				.body("[1].ccyCode", equalTo("EUR"))
				.body("[2].igpCode", equalTo("iguana"))
				.body("[2].ccyCode", equalTo("GBP"))
				.body("[3].igpCode", equalTo("iguana"))
				.body("[3].ccyCode", equalTo("mBTC"))
				.body("[4].igpCode", equalTo("newt"))
				.body("[4].ccyCode", equalTo("mBTC"));

	}

	@Test
	@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL, "INSERT INTO t_bigwin VALUES ('JPY', 20)"})
	@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
	public void okFilterBigWinsGroupedByCcyCode()
	{
		this.saveTxn(
				PlayerTxnBuilder.txn().withCcyCode("JPY").withAmount(BigDecimal.ZERO)
				.withType(TxnType.WIN).withTxnId("txnId8").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayerTxnBuilder.txn().withCcyCode("JPY").withAmount(BigDecimal.ONE)
				.withType(TxnType.WIN).withTxnId("txnId9").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayerTxnBuilder.txn().withCcyCode("JPY").withAmount(BigDecimal.TEN)
				.withType(TxnType.WIN).withTxnId("txnId10").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayerTxnBuilder.txn().withCcyCode("JPY").withAmount(new BigDecimal("30.00"))
				.withType(TxnType.WIN).withTxnId("txnId11").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayerTxnBuilder.txn().withCcyCode("JPY").withAmount(new BigDecimal("100.00"))
				.withType(TxnType.WIN).withTxnId("txnId12").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayerTxnBuilder.txn().withCcyCode("JPY").withAmount(BigDecimal.TEN)
				.withType(TxnType.STAKE).withTxnId("txnId13").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayerTxnBuilder.txn().withCcyCode("JPY").withAmount(new BigDecimal("30.00"))
				.withType(TxnType.STAKE).withTxnId("txnId14").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build()
				);

		//All non JPY reports are just the same as in other tests
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code")
		.queryParam("filterBigWins", true)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("JPY"))
		.body("[0].totalStake", equalTo(40.00f))
		.body("[0].totalWin", equalTo(130.00f))
		.body("[1].ccyCode", equalTo("EUR"))
		.body("[1].totalStake", equalTo(20.00f))
		.body("[1].totalWin", equalTo(1.00f))
		.body("[2].ccyCode", equalTo("GBP"))
		.body("[2].totalStake", equalTo(10.00f))
		.body("[2].totalWin", equalTo(0.00f))
		.body("[3].ccyCode", equalTo("mBTC"))
		.body("[3].totalStake", equalTo(10.00f))
		.body("[3].totalWin", equalTo(0.00f));

		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code")
		.queryParam("filterBigWins", false)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("JPY"))
		.body("[0].totalStake", equalTo(40.00f))
		.body("[0].totalWin", equalTo(141.00f))
		.body("[1].ccyCode", equalTo("EUR"))
		.body("[1].totalStake", equalTo(20.00f))
		.body("[1].totalWin", equalTo(1.00f))
		.body("[2].ccyCode", equalTo("GBP"))
		.body("[2].totalStake", equalTo(10.00f))
		.body("[2].totalWin", equalTo(0.00f))
		.body("[3].ccyCode", equalTo("mBTC"))
		.body("[3].totalStake", equalTo(10.00f))
		.body("[3].totalWin", equalTo(0.00f));
	}


	@Test
	public void okGroupByCcyCodeWhenBackwardsCompatibleReport()
	{
		RestAssured.given()
				.pathParam("igpCode", "iguana")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/igp/{igpCode}/txn/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].ccyCode", equalTo("EUR"))
				.body("[0].totalStake", equalTo(20.00f))
				.body("[0].totalWin", equalTo(1.00f))
				.body("[0].bonusCost", equalTo(0.00f))
				.body("[0].numFreeRounds", equalTo(0))
				.body("[0].uniquePlayers", equalTo(2))
				.body("[1].ccyCode", equalTo("GBP"))
				.body("[1].totalStake", equalTo(10.00f))
				.body("[1].totalWin", equalTo(0.00f))
				.body("[1].uniquePlayers", equalTo(1))
				.body("[1].bonusCost", equalTo(0.00f))
				.body("[1].numFreeRounds", equalTo(0))
				.body("[2].ccyCode", equalTo("mBTC"))
				.body("[2].totalStake", equalTo(10.00f))
				.body("[2].totalWin", equalTo(0.00f))
				.body("[2].uniquePlayers", equalTo(1))
				.body("[2].bonusCost", equalTo(0.00f))
				.body("[2].numFreeRounds", equalTo(0));
	}

	@Test
	public void givenMultipleIgpCodes_whenBackwardsCompatibleTxnReport_returnsCorrectResults()
	{
		RestAssured.given()
				.pathParam("igpCode","gecko")
				.queryParam("igpCodes", "iguana,gecko,newt")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/igp/{igpCode}/txn/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].igpCode", equalTo("gecko"))
				.body("[0].ccyCode", equalTo("mBTC"));
	}


	@Test
	public void okGroupByCcyCodeWithBonusInfo() {
		playRepository.save(PlayBuilder.play().withPlayId("1").withBonusFundType(OperatorBonusFundDetails.TYPE).build());

		saveTxn(
				TxnBuilder.txn().withPlayerId("player1").withPlayId("1").withCcyCode("EUR").withAmount(BigDecimal.TEN).withType(TxnType.STAKE).withTxnId("txnId1").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				TxnBuilder.txn().withPlayerId("player3").withPlayId("1").withCcyCode("GBP").withAmount(BigDecimal.TEN).withType(TxnType.STAKE).withTxnId("txnId4").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(4)).build()
				);

		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("EUR"))
		.body("[0].totalStake", equalTo(20.00f))
		.body("[0].totalWin", equalTo(1.00f))
		.body("[0].uniquePlayers", equalTo(2))
		.body("[0].bonusCost", equalTo(10.0f))
		.body("[0].numFreeRounds", equalTo(1))
		.body("[1].ccyCode", equalTo("GBP"))
		.body("[1].totalStake", equalTo(10.00f))
		.body("[1].totalWin", equalTo(0.00f))
		.body("[1].uniquePlayers", equalTo(1))
		.body("[1].bonusCost", equalTo(10.0f))
		.body("[1].numFreeRounds", equalTo(1));
	}

	@Test
	public void okGroupByCcyCodeCountryWithBonusInfo() {
		playRepository.save(PlayBuilder.play().withPlayId("1").withBonusFundType(OperatorBonusFundDetails.TYPE).build());

		saveTxn(
				TxnBuilder.txn().withPlayerId("player1").withPlayId("1").withCcyCode("EUR").withAmount(BigDecimal.TEN)
				.withType(TxnType.STAKE).withTxnId("txnId1").withStatus(TxnStatus.OK).withTxnTs(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build()
				);

		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code, country")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("EUR"))
		.body("[0].country", equalTo("FR"))
		.body("[0].totalStake", equalTo(10.00f))
		.body("[0].totalWin", equalTo(1.00f))
		.body("[0].bonusCost", equalTo(10.00f))
		.body("[0].numFreeRounds", equalTo(1))
		.body("[0].uniquePlayers", equalTo(1))
		.body("[1].ccyCode", equalTo("mBTC"))
		.body("[1].country", equalTo("CH"))
		.body("[1].totalStake", equalTo(10.00f))
		.body("[1].totalWin", equalTo(0.00f))
		.body("[1].uniquePlayers", equalTo(1))
		.body("[1].bonusCost", equalTo(00.00f))
		.body("[1].numFreeRounds", equalTo(0))
		.body("[2].ccyCode", equalTo("EUR"))
		.body("[2].country", equalTo("ES"))
		.body("[2].totalStake", equalTo(10.00f))
		.body("[2].totalWin", equalTo(0.00f))
		.body("[2].uniquePlayers", equalTo(1))
		.body("[2].bonusCost", equalTo(0.00f))
		.body("[2].numFreeRounds", equalTo(0))
				.body("[3].ccyCode", equalTo("GBP"))
				.body("[3].country", equalTo("GB"))
				.body("[3].totalStake", equalTo(10.00f))
				.body("[3].totalWin", equalTo(0.00f))
				.body("[3].uniquePlayers", equalTo(1))
				.body("[3].bonusCost", equalTo(0.00f))
				.body("[3].numFreeRounds", equalTo(0));
	}

	@Test
	public void okFilterCountry()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code")
		.queryParam("country","GB")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("GBP"))
		.body("[0].totalStake", equalTo(10.00f))
		.body("[0].totalWin", equalTo(0.00f))
		.body("[0].uniquePlayers", equalTo(1));
	}

	@Test
	public void okFilterPlayerId()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code")
		.queryParam("playerId","player3")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("GBP"))
		.body("[0].totalStake", equalTo(10.00f))
		.body("[0].totalWin", equalTo(0.00f))
		.body("[0].uniquePlayers", equalTo(1));
	}

	@Test
	public void noGroupBy()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(400);
	}

	@Test
	public void invalidDate()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom","This is not a date")
		.queryParam("dateTo", "This is not a date")
		.queryParam("groupBy", "ccy_code")
		.queryParam("invalidFilter","invalidFilter")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/txn/report")
		.then()
		.log().all()
		.statusCode(400);
	}

	@Test
	public void givenException_whenTxnReport_thenInternalServerExceptionThrown() {
		doThrow(new RuntimeException("test"))
				.when(namedParameterJdbcTemplate)
				.query(any(), any(SqlParameterSource.class), any(RowMapper.class));
		RestAssured.given()
				.queryParam("igpCodes", "gecko")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.queryParam("country","GB")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/txn/report")
				.then()
				.log().all()
				.statusCode(500)
				.body("msg",equalTo("Txn report failed."));
	}
}

