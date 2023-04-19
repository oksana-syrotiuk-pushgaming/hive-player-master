package io.gsi.hive.platform.player.api.s2s;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.game.cache.GameCacheConfig;
import io.gsi.hive.platform.player.play.search.PlaySearchArguments;
import io.gsi.hive.platform.player.play.search.PlaySearchRecord;
import io.gsi.hive.platform.player.play.search.PlaySearchRecordBuilder;
import io.gsi.hive.platform.player.play.search.PlaySearchService;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.session.Mode;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class S2SPlayControllerIT extends ApiITBase {

  @MockBean private PlayRepository playRepository;
  @MockBean private PlaySearchService playSearchService;

  @Autowired
  CacheConfig cacheConfig;
  @Autowired
  GameCacheConfig gameCacheConfig;

  @After
  public void clearCaches() {
      cacheConfig.getPlayCache().clear();

      Cache gameCache = gameCacheConfig
              .gameCacheManager()
              .getCache("gameCache");

      if (gameCache != null) {
          gameCache.clear();
      }
  }

  @Test
  public void givenValidPlayId_whenGetPlay_thenPlayReturned(){
    Play play = new Play();
    play.setPlayId("888-1015");
    when(playRepository.findById("888-1015")).thenReturn(Optional.of(play));
    
    RestAssured.given()
        .log().all()
        .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
        .pathParam("playId","888-1015")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/hive/s2s/platform/player/v1/play/{playId}")
        .then()
        .log().all()
        .statusCode(200)
        .body("playId", equalTo("888-1015"));
  }
  
  @Test
  public void givenParams_whenGetPlaysAfter_thenPlaysReturned() {
      ZonedDateTime dateFrom = ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC"));
      ZonedDateTime dateTo = ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC"));

      PlaySearchArguments playSearchArguments = PlaySearchArguments.builder()
              .playerId(PlayerPresets.PLAYERID)
              .igpCodes(List.of("iguana", "gecko"))
              .dateFrom(dateFrom)
              .dateTo(dateTo)
              .page(1)
              .pageSize(10)
              .guest(false)
              .mode(Mode.real)
              .build();

        mockPlaySearch(playSearchArguments);

        RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .queryParam("igpCodes", "iguana", "gecko")
                .queryParam("playerId", PlayerPresets.PLAYERID)
                .queryParam("dateFrom", dateFrom.toString())
                .queryParam("dateTo", dateTo.toString())
                .queryParam("pageSize", 10)
                .queryParam("page", 1)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/hive/s2s/platform/player/v1/plays")
                .then()
                .log().all()
                .statusCode(200)
                .body("[0].playId", equalTo("888-1015"))
                .body("[0].playerId", equalTo(PlayerPresets.PLAYERID))
                .body("[0].ccyCode", equalTo(PlayerPresets.CCY_CODE))
                .body("[0].createdAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
                .body("[0].modifiedAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
                .body("[0].gameCode", equalTo(GamePresets.CODE))
                .body("[0].guest", equalTo(false))
                .body("[0].igpCode", equalTo(IgpPresets.IGPCODE_IGUANA))
                .body("[0].numTxns", equalTo(0))
                .body("[0].status", equalTo(PlayStatus.ACTIVE.name()))
                .body("[0].stake", equalTo(BigDecimal.ZERO.intValue()))
                .body("[0].mode", equalTo(Mode.real.name()))
                .body("[0].win", equalTo(BigDecimal.ZERO.intValue()))
                .body("[0].playRef", equalTo("igp-play-id"))
                .body("[1].playId", equalTo("889-1015"))
                .body("[1].playId", equalTo("889-1015"))
                .body("[1].playerId", equalTo(PlayerPresets.PLAYERID))
                .body("[1].ccyCode", equalTo(PlayerPresets.CCY_CODE))
                .body("[1].createdAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
                .body("[1].modifiedAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
                .body("[1].gameCode", equalTo(GamePresets.CODE))
                .body("[1].guest", equalTo(false))
                .body("[1].igpCode", equalTo(IgpPresets.IGPCODE_IGUANA))
                .body("[1].numTxns", equalTo(0))
                .body("[1].status", equalTo(PlayStatus.ACTIVE.name()))
                .body("[1].stake", equalTo(BigDecimal.ZERO.intValue()))
                .body("[1].mode", equalTo(Mode.real.name()))
                .body("[1].win", equalTo(BigDecimal.ZERO.intValue()))
                .body("[1].playRef", equalTo("igp-play-id-2"));

        Mockito.verify(playSearchService).search(playSearchArguments);
  }

  @Test
  public void givenNullPage_whenGetPlaysAfter_thenPlaysOnPage0Returned(){
      ZonedDateTime dateFrom = ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC"));
      ZonedDateTime dateTo = ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC"));

      PlaySearchArguments playSearchArguments = PlaySearchArguments.builder()
              .playerId(PlayerPresets.PLAYERID)
              .igpCodes(List.of("iguana", "gecko"))
              .dateFrom(dateFrom)
              .dateTo(dateTo)
              .page(0)
              .pageSize(5)
              .guest(false)
              .mode(Mode.real)
              .build();

      mockPlaySearch(playSearchArguments);

      RestAssured.given()
              .log().all()
              .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
              .queryParam("igpCodes", "iguana", "gecko")
              .queryParam("playerId", PlayerPresets.PLAYERID)
              .queryParam("dateFrom", dateFrom.toString())
              .queryParam("dateTo", dateTo.toString())
              .queryParam("pageSize", 5)
              .contentType(ContentType.JSON)
              .accept(ContentType.JSON)
              .get("/hive/s2s/platform/player/v1/plays")
              .then()
              .log().all()
              .statusCode(200)
              .body("[0].playId", equalTo("888-1015"))
              .body("[0].playerId", equalTo(PlayerPresets.PLAYERID))
              .body("[0].ccyCode", equalTo(PlayerPresets.CCY_CODE))
              .body("[0].createdAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
              .body("[0].modifiedAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
              .body("[0].gameCode", equalTo(GamePresets.CODE))
              .body("[0].guest", equalTo(false))
              .body("[0].igpCode", equalTo(IgpPresets.IGPCODE_IGUANA))
              .body("[0].numTxns", equalTo(0))
              .body("[0].status", equalTo(PlayStatus.ACTIVE.name()))
              .body("[0].stake", equalTo(BigDecimal.ZERO.intValue()))
              .body("[0].mode", equalTo(Mode.real.name()))
              .body("[0].win", equalTo(BigDecimal.ZERO.intValue()))
              .body("[0].playRef", equalTo("igp-play-id"))
              .body("[1].playId", equalTo("889-1015"))
              .body("[1].playId", equalTo("889-1015"))
              .body("[1].playerId", equalTo(PlayerPresets.PLAYERID))
              .body("[1].ccyCode", equalTo(PlayerPresets.CCY_CODE))
              .body("[1].createdAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
              .body("[1].modifiedAt", equalTo(TimePresets.ZONEDEPOCHUTC.toInstant().toString()))
              .body("[1].gameCode", equalTo(GamePresets.CODE))
              .body("[1].guest", equalTo(false))
              .body("[1].igpCode", equalTo(IgpPresets.IGPCODE_IGUANA))
              .body("[1].numTxns", equalTo(0))
              .body("[1].status", equalTo(PlayStatus.ACTIVE.name()))
              .body("[1].stake", equalTo(BigDecimal.ZERO.intValue()))
              .body("[1].mode", equalTo(Mode.real.name()))
              .body("[1].win", equalTo(BigDecimal.ZERO.intValue()))
              .body("[1].playRef", equalTo("igp-play-id-2"));
      Mockito.verify(playSearchService).search(playSearchArguments);
  }

  private void mockPlaySearch(PlaySearchArguments playSearchArguments) {
      PlaySearchRecord play = PlaySearchRecordBuilder.aPlaySearchRecord()
              .withPlayId("888-1015")
              .build();
      PlaySearchRecord play2 = PlaySearchRecordBuilder.aPlaySearchRecord()
              .withPlayId("889-1015")
              .withPlayRef("igp-play-id-2")
              .build();

      PageRequest pageData = PageRequest.of(playSearchArguments.getPage(), playSearchArguments.getPageSize());
      when(playSearchService.search(Mockito.any()))
              .thenReturn(new PageImpl<>(List.of(play, play2), pageData, 2));
  }

  @Test
  public void givenValidPlayId_whenVoidPlay_thenPlayVoided(){
    Play play = new Play();
    play.setStatus(PlayStatus.ACTIVE);
    when(playRepository.findById("888-1015")).thenReturn(Optional.of(play));
    RestAssured.given()
        .log().all()
        .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
        .queryParam("playId","888-1015")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/hive/s2s/platform/player/v1/play/void")
        .then()
        .log().all()
        .statusCode(200)
        .body("status", equalTo(PlayStatus.VOIDED.toString()));
  }

  @Test
  public void givenInvalidPlayId_whenVoidPlay_thenPlayNotFoundException(){
    when(playRepository.findById("888-1015")).thenReturn(Optional.ofNullable(null));
    RestAssured.given()
        .log().all()
        .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
        .queryParam("playId","888-1015")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/hive/s2s/platform/player/v1/play/void")
        .then()
        .log().all()
        .statusCode(404)
        .body("code", equalTo("PlayNotFound"));
  }

  @Test
  public void givenPlayIdOfFinishedPlay_whenVoidPlay_thenPlayNotVoided(){
    Play play = new Play();
    play.setStatus(PlayStatus.FINISHED);
    when(playRepository.findById("888-1015")).thenReturn(Optional.of(play));
    RestAssured.given()
        .log().all()
        .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
        .queryParam("playId","888-1015")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/hive/s2s/platform/player/v1/play/void")
        .then()
        .log().all()
        .statusCode(409)
        .body("code", equalTo("InvalidState"));
  }

  @Test
  public void givenPlayIdOfVoidedPlay_whenVoidPlay_thenOK(){
    Play play = new Play();
    play.setStatus(PlayStatus.VOIDED);
    when(playRepository.findById("888-1015")).thenReturn(Optional.of(play));
    RestAssured.given()
        .log().all()
        .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
        .queryParam("playId","888-1015")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/hive/s2s/platform/player/v1/play/void")
        .then()
        .log().all()
        .statusCode(200)
        .body("status", equalTo(PlayStatus.VOIDED.toString()));
  }
}
