package io.gsi.hive.platform.player.api.bo;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.txn.TxnType;
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
public class PlayReportIT extends ApiITBase
{
	
	@Autowired @Qualifier(CacheConfig.PLAY_REPORT_CACHE_NAME)
	private CaffeineCache playReportCache;
	
	@Autowired
	private PlayRepository playRepository;
	
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
				PlayerBuilder.aPlayer().withCountry("GB")
						.withCcyCode("GBP").withPlayerId("player4").withIgpCode("gecko").build(),
				PlayerBuilder.aPlayer().withCountry("GB")
						.withCcyCode("GBP").withPlayerId("player5").withIgpCode("newt").build()
				);
		this.savePlays(
				PlayBuilder.play().withPlayerId("player1").withCcyCode("EUR").withStake(BigDecimal.TEN).withPlayId("playId1").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(1)).build(),
				PlayBuilder.play().withPlayerId("player1").withCcyCode("EUR").withWin(BigDecimal.ONE).withPlayId("playId2").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(2)).build(),
				PlayBuilder.play().withPlayerId("player2").withCcyCode("EUR").withStake(BigDecimal.TEN).withPlayId("playId3").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(3)).build(),
				PlayBuilder.play().withPlayerId("player3").withCcyCode("GBP").withStake(BigDecimal.TEN).withPlayId("playId4").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(4)).build(),
				PlayBuilder.play().withPlayerId("player4").withCcyCode("GBP").withStake(BigDecimal.TEN).withPlayId("playId5").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(5)).withIgpCode("gecko").build(),
				PlayBuilder.play().withPlayerId("player5").withCcyCode("GBP").withStake(BigDecimal.TEN).withPlayId("playId6").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(6)).withIgpCode("newt").build(),
				PlayBuilder.play().withPlayerId("player5").withCcyCode("GBP").withStake(BigDecimal.TEN).withPlayId("playId7").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(7)).withIgpCode("newt").build()
				);
		
		playReportCache.clear();
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
		.get("/hive/bo/platform/player/v1/play/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("EUR"))
		.body("[0].totalStake", equalTo(40.00f))
		.body("[0].totalWin", equalTo(21.0f))
		.body("[0].grossGamingRevenue",equalTo(19f))
		.body("[0].uniquePlayers", equalTo(2))
		.body("[1].ccyCode", equalTo("GBP"))
		.body("[1].totalStake", equalTo(10.00f))
		.body("[1].totalWin", equalTo(10.0f))
		.body("[1].grossGamingRevenue",equalTo(0f))
		.body("[1].uniquePlayers", equalTo(1));
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
				.get("/hive/bo/platform/player/v1/igp/{igpCode}/play/report")
				.then()
				.log().all()
				.statusCode(400);
	}

	@Test
	public void failOrderBySqlInjection()
	{
		RestAssured.given()
				.pathParam("igpCode", "iguana")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.queryParam("orderBy", "num_plays; \n drop table t_txn; --")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/igp/{igpCode}/play/report")
				.then()
				.log().all()
				.statusCode(400);
	}

	@Test
	public void givenMultipleIgpCodes_whenGetPlayReport_returnsCorrectResults()
	{
		RestAssured.given()
				.queryParam("igpCodes", "iguana,gecko,newt")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].igpCode", equalTo("gecko"))
				.body("[0].totalStake", equalTo(10.00f))
				.body("[0].totalWin", equalTo(10.0f))
				.body("[0].grossGamingRevenue",equalTo(0f))
				.body("[0].uniquePlayers", equalTo(1))
				.body("[1].igpCode", equalTo("iguana"))
				.body("[1].totalStake", equalTo(40.00f))
				.body("[1].totalWin", equalTo(21.0f))
				.body("[1].grossGamingRevenue",equalTo(19f))
				.body("[1].uniquePlayers", equalTo(2))
				.body("[2].igpCode", equalTo("iguana"))
				.body("[2].totalStake", equalTo(10.00f))
				.body("[2].totalWin", equalTo(10.0f))
				.body("[2].grossGamingRevenue",equalTo(0f))
				.body("[2].uniquePlayers", equalTo(1))
				.body("[3].numPlays", equalTo(2))
				.body("[3].igpCode", equalTo("newt"))
				.body("[3].totalStake", equalTo(20.00f))
				.body("[3].totalWin", equalTo(20.0f))
				.body("[3].grossGamingRevenue",equalTo(0f))
				.body("[3].uniquePlayers", equalTo(1));

	}

	@Test
	public void givenSingleIgpCode_whenGetPlayReport_returnsCorrectResults()
	{
		RestAssured.given()
				.queryParam("igpCodes", "gecko")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].igpCode", equalTo("gecko"))
				.body("[0].totalStake", equalTo(10.00f))
				.body("[0].totalWin", equalTo(10.0f))
				.body("[0].grossGamingRevenue",equalTo(0f))
				.body("[0].uniquePlayers", equalTo(1));
	}

	@Test
	public void givenSingleIgpCode_whenGetPlayReportWithFreeroundFilterEnabled_returnsCorrectResults() {

		//Non freerounds data
		this.saveTxn(
				TxnBuilder.txn().withTxnId("txnId1").withPlayId("playId4").build(),
				TxnBuilder.txn().withTxnId("txnId2").withPlayId("playId5").build()
		);

		saveOperatorFreeroundsPlays();

		RestAssured.given()
				.queryParam("igpCodes", "newt")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.queryParam("onlyFreeroundPlays", true)
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/report")
				.then()
				.statusCode(200)
				.body("[0].numPlays", equalTo(2))
				.body("[0].ccyCode", equalTo("GBP"))
				.body("[0].totalStake", equalTo(20.0f))
				.body("[0].totalWin", equalTo(20.0f))
				.body("[0].grossGamingRevenue", equalTo(0.00f))
				.body("[0].igpCode", equalTo("newt"));

	}

	@Test
	public void givenSingleIgpCode_whenGetPlayReportWithNoFreeroundFilter_returnsCorrectResults()
 	{
		this.saveTxn(
				TxnBuilder.txn().withTxnId("txnId1").build()
		);

		saveOperatorFreeroundsPlays();

		RestAssured.given()
				.queryParam("igpCodes", "newt")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/report")
				.then()
				.statusCode(200)
				.body("[0].numPlays", equalTo(4))
				.body("[0].ccyCode", equalTo("GBP"))
				.body("[0].totalStake", equalTo(40.0f))
				.body("[0].totalWin", equalTo(40.0f))
				.body("[0].grossGamingRevenue", equalTo(0.00f))
				.body("[0].igpCode", equalTo("newt"));

	}

	@Test
	public void givenSingleIgpCode_whenGetPlayReportWithFreeroundFilterDisabled_returnsCorrectResults()
	{
		this.saveTxn(
				TxnBuilder.txn().withTxnId("txnId1").withPlayId("playId5").withType(TxnType.OPFRSTK).build()
		);

		this.savePlays(
				PlayBuilder.play().withPlayerId("player4").withCcyCode("GBP").withStake(BigDecimal.TEN).withPlayId("playId11").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(5)).withIgpCode("gecko").build(),
				PlayBuilder.play().withPlayerId("player4").withCcyCode("GBP").withStake(BigDecimal.ONE).withPlayId("playId12").withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(5)).withIgpCode("gecko").build()
		);

		RestAssured.given()
				.queryParam("igpCodes", "gecko")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.queryParam("onlyFreeroundPlays", false)
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].numPlays", equalTo(2))
				.body("[0].ccyCode", equalTo("GBP"))
				.body("[0].totalStake", equalTo(11.0f))
				.body("[0].totalWin", equalTo(20.0f))
				.body("[0].grossGamingRevenue", equalTo(-9.00f))
				.body("[0].igpCode", equalTo("gecko"));
	}

	@Test
	public void okFilterCountry()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code","country")
		.queryParam("country","GB")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("GBP"))
		.body("[0].totalStake", equalTo(10.0f))
		.body("[0].totalWin", equalTo(10.0f))
		.body("[0].uniquePlayers", equalTo(1));
	}

	@Test
	public void givenFilterByCountry_CountryIsPresentInResponse()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code","country")
		.queryParam("country","GB")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("GBP"))
		.body("[0].totalStake", equalTo(10.0f))
		.body("[0].totalWin", equalTo(10.0f))
		.body("[0].uniquePlayers", equalTo(1))
		.body("[0].country",equalTo("GB"));
	}

	@Test
	public void givenFilterByCountryFR_ContryIsPresentInResponse()
	{
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.queryParam("groupBy", "ccy_code","country")
		.queryParam("country","FR")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("EUR"))
		.body("[0].totalStake", equalTo(30.0f))
		.body("[0].totalWin", equalTo(11.0f))
		.body("[0].uniquePlayers", equalTo(1))
		.body("[0].country",equalTo("FR"));
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
		.get("/hive/bo/platform/player/v1/play/report")
		.then()
		.log().all()
		.statusCode(200)
		.body("[0].ccyCode", equalTo("GBP"))
		.body("[0].totalStake", equalTo(10.00f))
		.body("[0].totalWin", equalTo(10.0f))
		.body("[0].grossGamingRevenue",equalTo(0f))
		.body("[0].uniquePlayers", equalTo(1));
	}

	@Test
	public void okWhenGetBackwardsCompatiblePlayReport()
	{
		RestAssured.given()
				.pathParam("igpCode", "iguana")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/igp/{igpCode}/play/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].ccyCode", equalTo("EUR"))
				.body("[0].totalStake", equalTo(40.00f))
				.body("[0].totalWin", equalTo(21.0f))
				.body("[0].grossGamingRevenue",equalTo(19f))
				.body("[0].uniquePlayers", equalTo(2))
				.body("[1].ccyCode", equalTo("GBP"))
				.body("[1].totalStake", equalTo(10.00f))
				.body("[1].totalWin", equalTo(10.0f))
				.body("[1].grossGamingRevenue",equalTo(0f))
				.body("[1].uniquePlayers", equalTo(1));
	}

	@Test
	public void givenMultipleIgpCodes_whenBackwardsCompatiblePlayReport_returnsCorrectResults()
	{
		RestAssured.given()
				.pathParam("igpCode", "newt")
				.queryParam("igpCodes", "iguana,gecko,newt")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/igp/{igpCode}/play/report")
				.then()
				.log().all()
				.statusCode(200)
				.body("[0].igpCode", equalTo("newt"))
				.body("[0].totalStake", equalTo(20.00f))
				.body("[0].totalWin", equalTo(20.0f))
				.body("[0].grossGamingRevenue",equalTo(0f))
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
		.get("/hive/bo/platform/player/v1/play/report")
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
		.get("/hive/bo/platform/player/v1/play/report")
		.then()
		.log().all()
		.statusCode(400);
	}

	@Test
	public void givenException_whenPlayReport_thenInternalServerExceptionThrown() {
		doThrow(new RuntimeException("test"))
				.when(namedParameterJdbcTemplate)
				.query(any(), any(SqlParameterSource.class), any(RowMapper.class));
		RestAssured.given()
				.queryParam("igpCodes", "gecko")
				.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
				.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
				.queryParam("groupBy", "ccy_code","country")
				.queryParam("country","GB")
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/report")
				.then()
				.log().all()
				.statusCode(500)
				.body("msg",equalTo("Play report failed."));
	}

	private void savePlays(Play... plays){
		playRepository.saveAll(Arrays.asList(plays));
	}

	private void saveOperatorFreeroundsPlays() {
		this.savePlays(
				PlayBuilder.play()
						.withPlayerId("player5")
						.withCcyCode("GBP")
						.withStake(BigDecimal.TEN)
						.withPlayId("playId8")
						.withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(7))
						.withIgpCode("newt")
						.build(),
				PlayBuilder.play()
						.withPlayerId("player5")
						.withCcyCode("GBP")
						.withStake(BigDecimal.TEN)
						.withPlayId("playId9")
						.withModifiedAt(TimePresets.ZONEDEPOCHUTC.plusDays(7))
						.withIgpCode("newt")
						.build()
		);
		this.saveTxn(
				TxnBuilder.txn()
						.withTxnId("txnId3")
						.withPlayId("playId8")
						.withType(TxnType.OPFRSTK)
						.build(),
				TxnBuilder.txn()
						.withTxnId("txnId4")
						.withPlayId("playId9")
						.withType(TxnType.OPFRSTK)
						.build()
		);
	}
}

