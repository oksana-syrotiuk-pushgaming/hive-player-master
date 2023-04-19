package io.gsi.hive.platform.player.api.bo;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL})
public class PlaySearchIT extends ApiITBase
{
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired @Qualifier(CacheConfig.PLAY_SEARCH_CACHE_NAME)
	private CaffeineCache playSearchCache;

	@Autowired
	private PlayRepository playRepository;

	@Before
	public void initialiseTestDefaults()
	{
		this.defaultZonedDateTime = TimePresets.ZONEDEPOCHUTC;
		this.defaultDateFrom = TimePresets.ZONEDEPOCHUTC.toString();
		this.defaultDateTo = TimePresets.ZONEDEPOCHUTC.plusDays(1).toString();

		playSearchCache.clear();
	}

	@Test
	public void failMandatoryConstraintViolation() {
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(400)
		.body("code",equalTo("BadRequest"));
	}


	@Test
	public void failDateRangeFilterExceedsMaximumPeriod() {
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",TimePresets.ZONEDEPOCHUTC.toString())
		.queryParam("dateTo", TimePresets.ZONEDEPOCHUTC.plusDays(32).toString())
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(400)
		.body("code",equalTo("BadRequest"));
	}

	@Test
	public void okQueryWithMandatoryFilters()
	{
		
		this.saveDefaultPlayer();
		this.savePlays(
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime)
				.withPlayId("1")
				.withMode(Mode.demo)
				.build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.withMode(Mode.demo)
				.build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayId("3")
				.build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.queryParam("mode", Mode.real)
		.queryParam("ccyCode", "GBP")
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId", equalTo("3"))
		.body("content[0].country", equalTo("GB"))
		.body("content[0].isFreeRound", equalTo(false));
	}

	@Test
	public void okQueryWithPlayerIdFilter()
	{
		
		this.savePlayer(
				PlayerBuilder.aPlayer().build(),
				PlayerBuilder.aPlayer().withPlayerId("playerOne").build()
				);
		this.savePlays(
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime)
				.withMode(Mode.real)
				.build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.withMode(Mode.real)
				.build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayId("3")
				.withPlayerId("playerOne")
				.withMode(Mode.real)
				.build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("playerId","playerOne")
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("3"))
		.body("content[0].playerId",equalTo("playerOne"));
	}

	@Test
	public void givenBonusTransaction_whenConductPlaySearch_returnBonusPlay() {
		this.saveDefaultPlayer();
		this.savePlays(
				PlayBuilder.play()
						.withModifiedAt(defaultZonedDateTime)
						.withPlayId("1")
						.build(),
				PlayBuilder.play()
						.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
						.withPlayId("2")
						.withBonusFundType("HIVE")
						.build()
		);

		this.saveTxn(
				TxnBuilder.txn().withTxnId("txnId2").build());

		assertThat(playRepository.count()).isEqualTo(2l);

		RestAssured.given()
				.queryParam("igpCodes", "iguana")
				.queryParam("dateFrom",this.defaultDateFrom)
				.queryParam("dateTo", this.defaultDateTo)
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/search")
				.then()
				.log().all()
				.statusCode(200)
				.body("content[0].playId", equalTo("1"))
				.body("content[0].isFreeRound", equalTo(false))
				.body("content[1].playId", equalTo("2"))
				.body("content[1].isFreeRound", equalTo(true));

	}

	// TODO Filter needs added
	@Ignore
	@Test
	public void okQueryWithUsernameFilter()
	{
		
		this.saveDefaultPlayer();
		this.savePlayer(
				PlayerBuilder.aPlayer()
				.withPlayerId("playerOne")
				.withUsername("playerOneUsername")
				.build()
				);
		this.savePlays(
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime)
				.withMode(Mode.real).build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.withPlayerId("player1")
				.withMode(Mode.real).build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayId("3")
				.withPlayerId("playerOne")
				.withMode(Mode.real).build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);

		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("username","playerOneUsername")
		.queryParam("mode",Mode.real)
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("3"))
		.body("content[0].username",equalTo("playerOneUsername"));
	}

	@Test
	public void okQueryWithGameCodeFilter()
	{
		this.saveDefaultPlayer();
		this.savePlayer(
				PlayerBuilder.aPlayer()
				.withPlayerId("playerOneDonkeyKongJr")
				.withUsername("playerOneDonkeyKongJrUsername")
				.build()
				);
		this.savePlays(
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime)
				.withMode(Mode.real).build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.withPlayerId("player1")
				.withMode(Mode.real)
				.build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayId("3")
				.withGameCode("donkeyKongJr")
				.withPlayerId("playerOneDonkeyKongJr")
				.withMode(Mode.real).build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("gameCode","donkeyKongJr")
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("3"))
		.body("content[0].gameCode",equalTo("donkeyKongJr"));
	}

	@Test
	public void okQueryWithModeFilter()
	{
		
		this.saveDefaultPlayer();
		this.saveDefaultPlay();
		this.savePlays(
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.withMode(Mode.demo)
				.build(),
				PlayBuilder.play()
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayId("3")
				.withMode(Mode.demo)
				.build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("mode","real")
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("1000-10"));
	}

	@Test
	public void okQueryWithPlayRefFilter()
	{
		
		this.saveDefaultPlayer();
		this.saveDefaultPlay();
		this.savePlays(
				PlayBuilder.play()
						.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
						.withPlayId("2")
						.withPlayRef(TxnPresets.PLAYREF)
						.build(),
				PlayBuilder.play()
						.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
						.withPlayId("3")
						.withPlayRef(TxnPresets.PLAYREF2)
						.build()
		);

		assertThat(playRepository.count()).isEqualTo(3l);

		RestAssured.given()
				.queryParam("igpCodes", "iguana")
				.queryParam("playRef",TxnPresets.PLAYREF)
				.queryParam("dateFrom",this.defaultDateFrom)
				.queryParam("dateTo", this.defaultDateTo)
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/search")
				.then()
				.log().all()
				.statusCode(200)
				.body("content[0].playId",equalTo("2"));
	}

	@Test
	public void okQueryWithGuestFilter()
	{
		
		this.saveDefaultPlayer();
		this.savePlayer(PlayerBuilder.aPlayer()
				.withGuest(true).withPlayerId("guestPlayer").build()
				);
		this.savePlays(
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime)
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayerId("guestPlayer")
				.withGuest(true)
				.withPlayId("3")
				.build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("guest",true)
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("3"))
		.body("content[0].guest",equalTo(true))
		.body("content[0].playerId",equalTo("guestPlayer"));
	}

	@Test
	public void okQueryWithStatusFilter()
	{
		
		this.saveDefaultPlayer();
		this.savePlays(
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime)
				.withStatus(PlayStatus.ACTIVE)
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withStatus(PlayStatus.FINISHED)
				.withPlayId("2")
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withStatus(PlayStatus.VOIDED)
				.withPlayId("3")
				.build()
				);
		
		assertThat(playRepository.count()).isEqualTo(3l);

		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("status", PlayStatus.VOIDED)
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("3"))
		.body("content[0].status",equalTo("VOIDED"));
	}

	@Test
	public void okQueryWithCcyCodeFilter()
	{
		
		this.saveDefaultPlayer();
		this.savePlayer(PlayerBuilder.aPlayer()
				.withCcyCode("USD")
				.withPlayerId("americanPlayer").build()
				);
		this.savePlays(
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime)
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayerId("americanPlayer")
				.withCcyCode("USD")
				.withPlayId("3")
				.build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("ccyCode","USD")
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("3"))
		.body("content[0].ccyCode",equalTo("USD"));
	}

	// TODO Filter needs added
	@Ignore
	@Test
	public void okQueryWithCountryFilter()
	{
		
		this.saveDefaultPlayer();
		this.savePlayer(PlayerBuilder.aPlayer()
				.withCountry("US")
				.withPlayerId("americanPlayer").build()
				);
		this.savePlays(
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime)
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayerId("americanPlayer")
				.withPlayId("3")
				.build()
				);

		assertThat(playRepository.count()).isEqualTo(3l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("country","US")
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("3"))
		.body("content[0].playerId",equalTo("americanPlayer"))
		.body("content[0].country",equalTo("US"));
	}

	@Test
	public void okQueryWithPlayIdFilter()
	{
		
		this.saveDefaultPlayer();
		
		this.savePlays(
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime)
				.withPlayId("1")
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(1))
				.withPlayId("2")
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(2))
				.withPlayId("3")
				.build(),
				PlayBuilder.play().withMode(Mode.real)
				.withModifiedAt(defaultZonedDateTime.plusMinutes(3))
				.withPlayId("4")
				.build()
				);

		assertThat(playRepository.count()).isEqualTo(4l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("playId","2")
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("2"));
	}

	@Test
	public void okQueryForResultsInPageZero()
	{
		
		this.savePlayersAndPlays(10);
		
		assertThat(playRepository.count()).isEqualTo(10l);
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("page","0")
		.queryParam("pageSize", 2)
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("0"))
		.body("content[1].playId",equalTo("1"))
		.body("number",equalTo(0))
		.body("size",equalTo(2))
		.body("first",equalTo(true))
		.body("last",equalTo(false));
	}

	@Test
	public void okQueryForResultsInPageOne()
	{
		
		this.savePlayersAndPlays(10);
		assertThat(playRepository.count()).isEqualTo(10l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("page","1")
		.queryParam("pageSize", 2)
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("2"))
		.body("content[1].playId",equalTo("3"))
		.body("number",equalTo(1))
		.body("size",equalTo(2))
		.body("first",equalTo(false))
		.body("last",equalTo(false));
	}

	@Test
	public void okQueryForResultsInLastPage()
	{
		
		this.savePlayersAndPlays(10);
		assertThat(playRepository.count()).isEqualTo(10l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("page", 4)
		.queryParam("pageSize", 2)
		.queryParam("dateFrom",this.defaultDateFrom)
		.queryParam("dateTo", this.defaultDateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("8"))
		.body("content[1].playId",equalTo("9"))
		.body("number",equalTo(4))
		.body("size",equalTo(2))
		.body("first",equalTo(false))
		.body("last",equalTo(true));
	}


	@Test
	public void okFindRecordsUsingDateFromAndDateToRangeFilter()
	{
		
		this.savePlayersAndPlays(10);

		ZonedDateTime startingDate = ZonedDateTime.parse("1970-01-01T00:00:00.000Z");
		String dateFrom = startingDate.plusMinutes(1).toString();
		String dateTo = startingDate.plusMinutes(5).toString();

		assertThat(playRepository.count()).isEqualTo(10l);
		
		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",dateFrom)
		.queryParam("dateTo", dateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("1"))
		.body("content[1].playId",equalTo("2"))
		.body("content[2].playId",equalTo("3"))
		.body("content[3].playId",equalTo("4"))
		.body("content[4].playId",equalTo("5"));
	}

	@Test
	public void okFindRecordsFromCacheUsingDateFromAndDateToRangeFilter()
	{
		
		this.savePlayersAndPlays(10);

		ZonedDateTime startingDate = ZonedDateTime.parse("1970-01-01T00:00:00.000Z");
		String dateFrom = startingDate.plusMinutes(1).toString();
		String dateTo = startingDate.plusMinutes(5).toString();

		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",dateFrom)
		.queryParam("dateTo", dateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("1"))
		.body("content[1].playId",equalTo("2"))
		.body("content[2].playId",equalTo("3"))
		.body("content[3].playId",equalTo("4"))
		.body("content[4].playId",equalTo("5"));

		jdbcTemplate.execute(PersistenceITBase.CLEAN_DB_SQL);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from t_play", Long.class)).isEqualTo(0);

		RestAssured.given()
		.queryParam("igpCodes", "iguana")
		.queryParam("dateFrom",dateFrom)
		.queryParam("dateTo", dateTo)
		.log().all()
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.get("/hive/bo/platform/player/v1/play/search")
		.then()
		.log().all()
		.statusCode(200)
		.body("content[0].playId",equalTo("1"))
		.body("content[1].playId",equalTo("2"))
		.body("content[2].playId",equalTo("3"))
		.body("content[3].playId",equalTo("4"))
		.body("content[4].playId",equalTo("5"));
	}

	@Test
	public void givenException_whenPlaySearch_thenInternalServerExceptionThrown() {
		savePlayersAndPlays(10);
		assertThat(playRepository.count()).isEqualTo(10l);
		RestAssured.given()
				.queryParam("igpCodes", "")
				.queryParam("playerId", 4)
				.queryParam("gameCode", "JTI1MjYl\n" +
						"MjUyMyUyNTM0JTI1MzUlMjUzQiUyNTMzJTI1MzAlMjUzNyUyNTMyJTI1\n" +
						"MjYlMjUyMyUyNTMzJTI1MzQlMjUzQiUyNTI2JTI1MjMlMjUzNCUyNTMxJ\n" +
						"TI1M0IlMjUyNiUyNTIzJTI1MzMlMjUzMiUyNTNCJTI1NEYlMjU1MiUyNTI2J\n" +
						"TI1MjMlMjUzMyUyNTMyJTI1M0IlMjUyNiUyNTIzJTI1MzQlMjUzMCUyNT\n" +
						"NCJTI1NTMlMjU0NSUyNTRDJTI1NDUlMjU0MyUyNTU0JTI1MjYlMjUyMy\n" +
						"UyNTMzJTI1MzIlMjUzQiUyNTI2JTI1MjMlMjUzNCUyNTMwJTI1M0IlMjU0\n" +
						"MyUyNTQxJTI1NTMlMjU0NSUyNTI2JTI1MjMlMjUzMyUyNTMyJTI1M0Il\n" +
						"MjU1NyUyNTQ4JTI1NDUlMjU0RSUyNTI2JTI1MjMlMjUzMyUyNTMyJTI1\n" +
						"M0IlMjUyNiUyNTIzJTI1MzQlMjUzMCUyNTNCJTI1MzYlMjUzMiUyNTMxJT\n" +
						"I1MzclMjUyNiUyNTIzJTI1MzYlMjUzMSUyNTNCJTI1MzQlMjUzMiUyNTM4\n" +
						"JTI1MzMlMjUyNiUyNTIzJTI1MzQlMjUzMSUyNTNCJTI1MjYlMjUyMyUyNT\n" +
						"MzJTI1MzIlMjUzQiUyNTU0JTI1NDglMjU0NSUyNTRFJTI1MjYlMjUyMyUyN\n" +
						"TMzJTI1MzIlMjUzQiUyNTRFJTI1NTUlMjU0QyUyNTRDJTI1MjYlMjUyMyUy\n" +
						"NTMzJTI1MzIlMjUzQiUyNTQ1JTI1NEMlMjU1MyUyNTQ1JTI1MjYlMjUyM\n" +
						"yUyNTMzJTI1MzIlMjUzQiUyNTQzJTI1NDElMjU1MyUyNTU0JTI1MjYlMjUy\n" +
						"MyUyNTM0JTI1MzAlMjUzQiUyNTI2JTI1MjMlMjUzNCUyNTMwJTI1M0IlM\n" +
						"jU0MyUyNTQ4JTI1NTIlMjUyNiUyNTIzJTI1MzQlMjUzMCUyNTNCJTI1MzEl\n" +
						"MjUzMCUyNTM1JTI1MjYlMjUyMyUyNTM0JTI1MzElMjUzQiUyNTI2JTI1M\n" +
						"jMlMjUzMSUyNTMyJTI1MzQlMjUzQiUyNTI2JTI1MjMlMjUzMSUyNTMyJT\n" +
						"I1MzQlMjUzQiUyNTQzJTI1NDglMjU1MiUyNTI2JTI1MjMlMjUzNCUyNTM\n" +
						"wJTI1M0IlMjUzNyUyNTM5JTI1MjYlMjUyMyUyNTM0JTI1MzElMjUzQiUyN\n" +
						"TI2JTI1MjMlMjUzMSUyNTMyJTI1MzQlMjUzQiUyNTI2JTI1MjMlMjUzMSU\n" +
						"yNTMyJTI1MzQlMjUzQiUyNTQzJTI1NDglMjU1MiUyNTI2JTI1MjMlMjUzN\n" +
						"CUyNTMwJTI1M0IlMjUzNiUyNTM3JTI1MjYlMjUyMyUyNTM0JTI1MzElMj\n" +
						"UzQiUyNTI2JTI1MjMlMjUzMSUyNTMyJTI1MzQlMjUzQiUyNTI2JTI1MjMl\n" +
						"MjUzMSUyNTMyJTI1MzQlMjUzQiUyNTQzJTI1NDglMjU1MiUyNTI2JTI1Mj\n" +
						"MlMjUzNCUyNTMwJTI1M0IlMjUzMSUyNTMwJTI1MzclMjUyNiUyNTIzJTI\n" +
						"1MzQlMjUzMSUyNTNCJTI1MjYlMjUyMyUyNTM0JTI1MzElMjUzQiUyNTI2\n" +
						"JTI1MjMlMjUzMyUyNTMyJTI1M0IlMjU0MSUyNTUzJTI1MjYlMjUyMyUyN\n" +
						"TMzJTI1MzIlMjUzQiUyNTRFJTI1NTUlMjU0RCUyNTQ1JTI1NTIlMjU0OSUy\n" +
						"NTQzJTI1MjYlMjUyMyUyNTM0JTI1MzElMjUzQiUyNTI2JTI1MjMlMjUzMy\n" +
						"UyNTMyJTI1M0IlMjU0NSUyNTRFJTI1NDQlMjUyNiUyNTIzJTI1MzQlMjUz\n" +
						"MSUyNTNCJTI1MjYlMjUyMyUyNTM0JTI1MzElMjUzQiUyNTI2JTI1MjMlMj\n" +
						"UzMyUyNTMyJTI1M0IlMjU0OSUyNTUzJTI1MjYlMjUyMyUyNTMzJTI1MzIl\n" +
						"MjUzQiUyNTRFJTI1NTUlMjU0QyUyNTRDJTI1MjYlMjUyMyUyNTMzJTI1M\n" +
						"zIlMjUzQiUyNTQxJTI1NEUlMjU0NCUyNTI2JTI1MjMlMjUzMyUyNTMyJTI1\n" +
						"M0IlMjUyNiUyNTIzJTI1MzQlMjUzMCUyNTNCJTI1MjYlMjUyMyUyNTMzJT\n" +
						"I1MzQlMjUzQiUyNTY3JTI1NDIlMjU3OCUyNTQzJTI1MjYlMjUyMyUyNTMz\n" +
						"JTI1MzQlMjUzQiUyNTI2JTI1MjMlMjUzMyUyNTMyJTI1M0IlMjU0QyUyNT\n" +
						"Q5JTI1NEIlMjU0NSUyNTI2JTI1MjMlMjUzMyUyNTMyJTI1M0IlMjUyNiUyN\n" +
						"TIzJTI1MzMlMjUzNCUyNTNCJTI1NjclMjU0MiUyNTc4JTI1NDM%C0%BD")
				.queryParam("mode", "demo")
				.queryParam("guest", true)
				.queryParam("status", "VOIDED")
				.queryParam("ccyCode", 1)
				.queryParam("playId", 2)
				.queryParam("playRef", 3)
				.queryParam("page",0)
				.queryParam("pageSize", 20)
				.queryParam("dateFrom",this.defaultDateFrom)
				.queryParam("dateTo", this.defaultDateTo)
				.log().all()
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.get("/hive/bo/platform/player/v1/play/search")
				.then()
				.log().all()
				.statusCode(500)
				.body("msg",equalTo("Play search failed."));
	}

	private void saveDefaultPlay()
	{
		playRepository.save(PlayBuilder.play().build());
	}

	private void savePlays(Play... plays)
	{
		playRepository.saveAll(Arrays.asList(plays));
	}
	
	protected void savePlayersAndPlays(int numRecords)
	{
		int startId = 0;
		int maxRecords = numRecords;

		for(int i=startId;i<maxRecords;i++)
		{
			playerRepository.save(PlayerBuilder
					.aPlayer()
					.withPlayerId("player"+i)
					.withUsername("player"+i)
					.build());
		}

		for(int i=startId;i<maxRecords;i++)
		{
			playRepository.save(PlayBuilder.play()
					.withMode(Mode.real)
					.withPlayId(String.valueOf(i))
					.withPlayerId("player"+i)
					.withModifiedAt(defaultZonedDateTime.plusMinutes(i))
					.build());
		}
	}
}
