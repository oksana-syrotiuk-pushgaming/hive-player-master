package io.gsi.hive.platform.player.api.bo;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.jdbc.Sql;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL})
public class TxnSearchIT extends ApiITBase {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    protected PlayRepository playRepository;

    @Before
    public void initialiseTestDefaults()
    {
        this.defaultZonedDateTime = TimePresets.ZONEDEPOCHUTC;
        this.defaultDateFrom = TimePresets.ZONEDEPOCHUTC.toString();
        this.defaultDateTo = TimePresets.ZONEDEPOCHUTC.plusMinutes(99).toString();
    }

    @Test
    public void failMandatoryConstraintViolation() {
        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
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
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(400)
                .body("code",equalTo("BadRequest"));
    }

    @Test
    public void okQueryWithMandatoryFilters()
    {
        this.saveDefaultPlayer();
        this.saveTxn(
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime)
                        .withTxnId("1")
                        .withMode(Mode.demo)
                        .build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withMode(Mode.demo)
                        .build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .queryParam("mode", Mode.real)
                .queryParam("ccyCode", "GBP")
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId", equalTo("3"));
    }

    @Test
    public void okQueryWithBonusFilterFalse()
    {
        this.saveDefaultPlayer();

        playRepository.saveAll(Arrays.asList(
            PlayBuilder.play()
                .withBonusFundType(OperatorBonusFundDetails.TYPE)
                .withPlayId("1000-10")
                .build(),
            PlayBuilder.play()
                .withBonusFundType(OperatorBonusFundDetails.TYPE)
                .withPlayId("1000-30")
                .build()));

        this.saveTxn(
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime)
                .withTxnId("1")
                .withPlayId("1000-10")
                .withCcyCode("RUB")
                .withStatus(TxnStatus.OK)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                .withTxnId("2")
                .withPlayId("1000-20")
                .withCcyCode("RUB")
                .withStatus(TxnStatus.PENDING)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                .withTxnId("3")
                .withPlayId("1000-30")
                .withCcyCode("RUB")
                .withStatus(TxnStatus.FAILED)
                .build()
        );

        RestAssured.given()
            .queryParam("igpCodes", "iguana")
            .queryParam("dateFrom",this.defaultDateFrom)
            .queryParam("dateTo", this.defaultDateTo)
            .queryParam("ccyCode", "RUB")
            .queryParam("onlyFreeroundTxns", false)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content[0].txnId", equalTo("2"))
            .body("content[0].status",equalTo("PENDING"));
    }

    @Test
    public void okQueryWithBonusFilterTrue()
    {
      this.saveDefaultPlayer();

      playRepository.saveAll(Arrays.asList(
            PlayBuilder.play()
                .withBonusFundType(OperatorBonusFundDetails.TYPE)
                .withPlayId("1000-10")
                .build(),
            PlayBuilder.play()
                .withBonusFundType(OperatorBonusFundDetails.TYPE)
                .withPlayId("1000-30")
                .build()));

        this.saveTxn(
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime)
                .withTxnId("1")
                .withPlayId("1000-10")
                .withCcyCode("EUR")
                .withStatus(TxnStatus.OK)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                .withTxnId("2")
                .withPlayId("1000-20")
                .withCcyCode("EUR")
                .withStatus(TxnStatus.PENDING)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                .withTxnId("3")
                .withPlayId("1000-30")
                .withCcyCode("EUR")
                .withStatus(TxnStatus.FAILED)
                .build()
        );

        RestAssured.given()
            .queryParam("igpCodes", "iguana")
            .queryParam("dateFrom",this.defaultDateFrom)
            .queryParam("dateTo", this.defaultDateTo)
            .queryParam("ccyCode", "EUR")
            .queryParam("onlyFreeroundTxns", true)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content[0].txnId", equalTo("1"))
            .body("content[0].status",equalTo("OK"))
            .body("content[1].txnId", equalTo("3"))
            .body("content[1].status",equalTo("FAILED"));

    }

    @Test
    public void okQueryWithNoBonusFilter()
    {
        this.saveDefaultPlayer();

        playRepository.saveAll(Arrays.asList(
            PlayBuilder.play()
                .withBonusFundType(OperatorBonusFundDetails.TYPE)
                .withPlayId("1000-10")
                .build(),
            PlayBuilder.play()
                .withBonusFundType(OperatorBonusFundDetails.TYPE)
                .withPlayId("1000-30")
                .build()));

        this.saveTxn(
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime)
                .withTxnId("1")
                .withPlayId("1000-10")
                .withCcyCode("CAD")
                .withStatus(TxnStatus.OK)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                .withTxnId("2")
                .withPlayId("1000-20")
                .withCcyCode("CAD")
                .withStatus(TxnStatus.PENDING)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                .withTxnId("3")
                .withPlayId("1000-30")
                .withCcyCode("CAD")
                .withStatus(TxnStatus.FAILED)
                .build()
        );



        RestAssured.given()
            .queryParam("igpCodes", "iguana")
            .queryParam("dateFrom",this.defaultDateFrom)
            .queryParam("dateTo", this.defaultDateTo)
            .queryParam("ccyCode", "CAD")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content[0].txnId", equalTo("1"))
            .body("content[0].status",equalTo("OK"))
            .body("content[1].txnId", equalTo("2"))
            .body("content[1].status",equalTo("PENDING"))
            .body("content[2].txnId", equalTo("3"))
            .body("content[2].status",equalTo("FAILED"));

    }

    @Test
    public void givenMultipleIgpCodes_whenQueryWithMandatoryFilters_returnsCorrectResults()
    {
        this.savePlayer(
            PlayerBuilder.aPlayer().build(),
            PlayerBuilder.aPlayer().withPlayerId("player2").withIgpCode("gecko").build(),
            PlayerBuilder.aPlayer().withPlayerId("player3").withIgpCode("newt").build()
        );
        this.saveTxn(
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime)
                .withTxnId("2")
                .withMode(Mode.demo)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                .withTxnId("3")
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                .withTxnId("4")
                .withIgpCode("gecko")
                .withPlayerId("player2")
                .withMode(Mode.demo)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(3))
                .withTxnId("5")
                .withIgpCode("gecko")
                .withPlayerId("player2")
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(4))
                .withTxnId("6")
                .withIgpCode("newt")
                .withPlayerId("player3")
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(5))
                .withTxnId("7")
                .withIgpCode("newt")
                .withPlayerId("player3")
                .withMode(Mode.demo)
                .build()
        );

        RestAssured.given()
            .queryParam("igpCodes", "iguana,gecko,newt")
            .queryParam("dateFrom",this.defaultDateFrom)
            .queryParam("dateTo", this.defaultDateTo)
            .queryParam("mode", Mode.real)
            .queryParam("ccyCode", "GBP")
            .log().all()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content[0].igpCode", equalTo("gecko"))
            .body("content[0].txnId", equalTo("5"))
            .body("content[1].igpCode", equalTo("iguana"))
            .body("content[1].txnId", equalTo("3"))
            .body("content[2].igpCode", equalTo("newt"))
            .body("content[2].txnId", equalTo("6"));
    }

    @Test
    public void okBackwardsCompatibleQueryWithMandatoryFilters()
    {
        this.saveDefaultPlayer();
        this.saveTxn(
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime)
                .withTxnId("1")
                .withMode(Mode.demo)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                .withTxnId("2")
                .withMode(Mode.demo)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                .withTxnId("3")
                .build()
        );

        RestAssured.given()
            .pathParam("igpCode", "iguana")
            .queryParam("dateFrom",this.defaultDateFrom)
            .queryParam("dateTo", this.defaultDateTo)
            .queryParam("mode", Mode.real)
            .queryParam("ccyCode", "GBP")
            .log().all()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/igp/{igpCode}/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content[0].txnId", equalTo("3"));
    }

    @Test
    public void givenMultipleIgpCodes_whenBackwardsCompatibleQueryWithMandatoryFilters_returnsCorrectResults()
    {
        this.savePlayer(
            PlayerBuilder.aPlayer().build(),
            PlayerBuilder.aPlayer().withPlayerId("player2").withIgpCode("gecko").build(),
            PlayerBuilder.aPlayer().withPlayerId("player3").withIgpCode("newt").build()
        );
        this.saveTxn(
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime)
                .withTxnId("2")
                .withMode(Mode.demo)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                .withTxnId("3")
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                .withTxnId("4")
                .withIgpCode("gecko")
                .withPlayerId("player2")
                .withMode(Mode.demo)
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(3))
                .withTxnId("5")
                .withIgpCode("gecko")
                .withPlayerId("player2")
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(4))
                .withTxnId("6")
                .withIgpCode("newt")
                .withPlayerId("player3")
                .build(),
            TxnBuilder.txn()
                .withTxnTs(defaultZonedDateTime.plusMinutes(5))
                .withTxnId("7")
                .withIgpCode("newt")
                .withPlayerId("player3")
                .withMode(Mode.demo)
                .build()
        );

        RestAssured.given()
            .pathParam("igpCode","newt")
            .queryParam("igpCodes", "iguana,gecko,newt")
            .queryParam("dateFrom",this.defaultDateFrom)
            .queryParam("dateTo", this.defaultDateTo)
            .queryParam("mode", Mode.real)
            .queryParam("ccyCode", "GBP")
            .log().all()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/igp/{igpCode}/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content[0].igpCode", equalTo("newt"))
            .body("content[0].txnId", equalTo("6"));
    }

    @Test
    public void okQueryWithPlayerIdFilter()
    {
        this.savePlayer(
                PlayerBuilder.aPlayer().build(),
                PlayerBuilder.aPlayer().withPlayerId("playerOne").build()
        );
        this.saveTxn(
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime)
                        .withMode(Mode.real)
                        .build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withMode(Mode.real)
                        .build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .withPlayerId("playerOne")
                        .withMode(Mode.real)
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("playerId","playerOne")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("3"))
                .body("content[0].playerId",equalTo("playerOne"));
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
        this.saveTxn(
                TxnBuilder.txn().withTxnTs(defaultZonedDateTime)
                        .withMode(Mode.real).build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withPlayerId("player1")
                        .withMode(Mode.real)
                        .build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .withGameCode("donkeyKongJr")
                        .withPlayerId("playerOneDonkeyKongJr").withMode(Mode.real).build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("gameCode","donkeyKongJr")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("3"))
                .body("content[0].gameCode",equalTo("donkeyKongJr"));
    }

    @Test
    public void okQueryWithModeFilter()
    {

        this.saveDefaultPlayer();
        this.saveDefaultTxn();
        this.saveTxn(
                TxnBuilder.txn().withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withMode(Mode.demo)
                        .build(),
                TxnBuilder.txn().withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .withMode(Mode.demo)
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("mode","real")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("1000-1"));
    }

    @Test
    public void okQueryWithGuestFilter()
    {

        this.saveDefaultPlayer();
        this.savePlayer(PlayerBuilder.aPlayer()
                .withGuest(true).withPlayerId("guestPlayer").build()
        );
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withPlayerId("guestPlayer")
                        .withGuest(true)
                        .withTxnId("3")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("guest",true)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("3"))
                .body("content[0].guest",equalTo(true))
                .body("content[0].playerId",equalTo("guestPlayer"));
    }

    @Test
    public void okQueryWithTypeFilter()
    {

        this.saveDefaultPlayer();
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .withType(TxnType.WIN)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withType(TxnType.STAKE)
                        .withTxnId("1000-2")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withType(TxnType.REFUND)
                        .withTxnId("1000-3")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("type", TxnType.STAKE)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("1000-2"))
                .body("content[0].type",equalTo("STAKE"));
    }

    @Test
    public void okQueryWithStatusFilter()
    {

        this.saveDefaultPlayer();
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .withStatus(TxnStatus.FAILED)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withStatus(TxnStatus.PENDING)
                        .withTxnId("2")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withStatus(TxnStatus.OK)
                        .withTxnId("3")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("status",TxnStatus.FAILED)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("1000-1"))
                .body("content[0].status",equalTo("FAILED"));
    }

    @Test
    public void okQueryWithCcyCodeFilter()
    {

        this.saveDefaultPlayer();
        this.savePlayer(PlayerBuilder.aPlayer()
                .withCcyCode("USD")
                .withPlayerId("americanPlayer").build()
        );
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withPlayerId("americanPlayer")
                        .withCcyCode("USD")
                        .withTxnId("3")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("ccyCode","USD")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("3"))
                .body("content[0].ccyCode",equalTo("USD"));
    }

    @Test
    public void okQueryWithCountryFilter()
    {

        this.saveDefaultPlayer();
        this.savePlayer(PlayerBuilder.aPlayer()
                .withCountry("US")
                .withPlayerId("americanPlayer").build()
        );
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withPlayerId("americanPlayer")
                        .withTxnId("3")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("country","US")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("3"))
                .body("content[0].playerId",equalTo("americanPlayer"))
                .body("content[0].country",equalTo("US"));
    }

    @Test
    public void okQueryWithTxnIdFilter()
    {

        this.saveDefaultPlayer();
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("txnId","3")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId", equalTo("3"));
    }

    @Test
    public void okQueryWithTxnRefFilter() {
        this.saveDefaultPlayer();
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withTxnRef(TxnPresets.TXNREF)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .withTxnRef(TxnPresets.TXNREF2)
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("txnRef", TxnPresets.TXNREF2)
                .queryParam("dateFrom", this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnRef", equalTo(TxnPresets.TXNREF2))
                .body("totalPages", equalTo(1));
    }

    @Test
    public void okQueryWithPlayRefFilter() {
        this.saveDefaultPlayer();
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .withPlayRef(TxnPresets.PLAYREF_NULL)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withPlayRef(TxnPresets.PLAYREF)
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .withPlayRef(TxnPresets.PLAYREF2)
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("playRef", TxnPresets.PLAYREF2)
                .queryParam("dateFrom", this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].playId", equalTo("1000-10"))
                .body("totalElements", equalTo(1));
    }

    @Test
    public void okQueryWithPlayIdFilter() {

        this.saveDefaultPlayer();
        this.saveTxn(
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime)
                        .withPlayId("1")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withPlayId("1")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .withPlayId("2")
                        .build(),
                TxnBuilder.txn().withMode(Mode.real)
                        .withTxnTs(defaultZonedDateTime.plusMinutes(3))
                        .withTxnId("4")
                        .withPlayId("2")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("playId","2")
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("3"))
                .body("content[0].playId",equalTo("2"))
                .body("content[1].txnId",equalTo("4"))
                .body("content[1].playId",equalTo("2"));
    }

    @Test
    public void okQueryForResultsInPageZero()
    {

        this.savePlayersAndTxns();
        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("page","0")
                .queryParam("pageSize", 2)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("0"))
                .body("content[1].txnId",equalTo("1"))
                .body("number",equalTo(0))
                .body("size",equalTo(2))
                .body("first",equalTo(true))
                .body("last",equalTo(false));
    }

    @Test
    public void okQueryForResultsInPageOne()
    {

        this.savePlayersAndTxns();
        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("page","1")
                .queryParam("pageSize", 2)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("2"))
                .body("content[1].txnId",equalTo("3"))
                .body("number",equalTo(1))
                .body("size",equalTo(2))
                .body("first",equalTo(false))
                .body("last",equalTo(false));
    }

    @Test
    public void okQueryForResultsInLastPage()
    {

        this.savePlayersAndTxns();
        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("page","19")
                .queryParam("pageSize", 5)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("95"))
                .body("content[1].txnId",equalTo("96"))
                .body("content[2].txnId",equalTo("97"))
                .body("content[3].txnId",equalTo("98"))
                .body("content[4].txnId",equalTo("99"))
                .body("number",equalTo(19))
                .body("size",equalTo(5))
                .body("first",equalTo(false))
                .body("last",equalTo(true));
    }


    @Test
    public void okFindRecordsUsingDateFromAndDateToRangeFilter()
    {

        this.savePlayersAndTxns();

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
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("1"))
                .body("content[1].txnId",equalTo("2"))
                .body("content[2].txnId",equalTo("3"))
                .body("content[3].txnId",equalTo("4"))
                .body("content[4].txnId",equalTo("5"));
    }

    @Test
    public void okFindRecordsFromCacheUsingDateFromAndDateToRangeFilter()
    {

        this.savePlayersAndTxns();

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
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("1"))
                .body("content[1].txnId",equalTo("2"))
                .body("content[2].txnId",equalTo("3"))
                .body("content[3].txnId",equalTo("4"))
                .body("content[4].txnId",equalTo("5"));

        jdbcTemplate.execute(PersistenceITBase.CLEAN_DB_SQL);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from t_txn", Long.class)).isEqualTo(0);

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("dateFrom",dateFrom)
                .queryParam("dateTo", dateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content[0].txnId",equalTo("1"))
                .body("content[1].txnId",equalTo("2"))
                .body("content[2].txnId",equalTo("3"))
                .body("content[3].txnId",equalTo("4"))
                .body("content[4].txnId",equalTo("5"));
    }

    @Test
    public void givenCachebuster_whenTxnSearch_doesNotReturnFromCache()
    {

        this.savePlayersAndTxns();

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
            .get("/hive/bo/platform/player/v1/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content.size()", is(not(0)));

        jdbcTemplate.execute(PersistenceITBase.CLEAN_DB_SQL);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from t_txn", Long.class)).isEqualTo(0);

        RestAssured.given()
            .queryParam("igpCodes", "iguana")
            .queryParam("dateFrom",dateFrom)
            .queryParam("dateTo", dateTo)
            .queryParam("cacheBuster", "cacheBust")
            .log().all()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/txn/search")
            .then()
            .log().all()
            .statusCode(200)
            .body("content.size()", is(0));
    }

    @Test
    public void okQueryWithUsernameFilter_ReturnsEmpty()
    {

        this.saveDefaultPlayer();
        this.savePlayer(
                PlayerBuilder.aPlayer()
                        .withPlayerId("playerOne")
                        .withUsername("playerOneUsername")
                        .build()
        );
        this.saveTxn(
                TxnBuilder.txn().withTxnTs(defaultZonedDateTime)
                        .withMode(Mode.real).build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(1))
                        .withTxnId("2")
                        .withPlayerId("player1")
                        .withMode(Mode.real).build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime.plusMinutes(2))
                        .withTxnId("3")
                        .withPlayerId("playerOne")
                        .withMode(Mode.real).build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("username","playerOneUsername")
                .queryParam("mode",Mode.real)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content.size()", is(0));
    }

    @Test
    public void givenTxnWithAccessToken_whenFilterByAccessToken_returnTxn() {
        this.savePlayer(
                PlayerBuilder.aPlayer()
                        .withPlayerId("playerOne")
                        .withAccessToken("abc")
                        .build(),
                PlayerBuilder.aPlayer()
                        .withPlayerId("playerTwo")
                        .withAccessToken("xyz")
                        .build()
        );
        this.saveTxn(
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime)
                        .withTxnId("1")
                        .withIgpCode("iguana")
                        .withPlayerId("playerOne")
                        .withMode(Mode.real)
                        .withAccessToken("abc")
                        .build(),
                TxnBuilder.txn()
                        .withTxnTs(defaultZonedDateTime)
                        .withTxnId("2")
                        .withIgpCode("iguana")
                        .withPlayerId("playerTwo")
                        .withMode(Mode.real)
                        .withAccessToken("xyz")
                        .build()
        );

        RestAssured.given()
                .queryParam("igpCodes", "iguana")
                .queryParam("accessToken","abc")
                .queryParam("mode",Mode.real)
                .queryParam("dateFrom",this.defaultDateFrom)
                .queryParam("dateTo", this.defaultDateTo)
                .log().all()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/bo/platform/player/v1/txn/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("content.size()", is(1))
                .body("content[0].accessToken",equalTo("abc"));
    }

    @Test
    public void givenException_whenTxnSearch_thenInternalServerExceptionThrown() {
        doThrow(new RuntimeException("test"))
            .when(namedParameterJdbcTemplate)
            .query(any(), any(SqlParameterSource.class), any(RowMapper.class));
        RestAssured.given()
            .queryParam("igpCodes", "gecko")
            .queryParam("page","0")
            .queryParam("pageSize", 2)
            .queryParam("dateFrom",this.defaultDateFrom)
            .queryParam("dateTo", this.defaultDateTo)
            .log().all()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .get("/hive/bo/platform/player/v1/txn/search")
            .then()
            .log().all()
            .statusCode(500)
            .body("msg",equalTo("Txn search failed."));
    }
}
