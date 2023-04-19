package io.gsi.hive.platform.player.api.s2s;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.GameplaySessionRequestBuilder;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerRepository;
import io.gsi.hive.platform.player.session.GameplaySession;
import io.gsi.hive.platform.player.session.GameplaySessionRequest;
import io.gsi.hive.platform.player.session.SessionRepository;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.session.SessionStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@Sql(statements = PersistenceITBase.CLEAN_DB_SQL, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class S2SGameplaySessionControllerIT extends ApiITBase {

    private static final String BASE_URI = "/hive/s2s/platform/player/v2";

    @SpyBean
    private SessionRepository sessionRepository;

    @SpyBean
    private PlayerRepository playerRepository;

    @Autowired
    private SessionService sessionService;

    @Before
    public void resetMocks() {
        reset(sessionRepository);
        reset(playerRepository);
    }

    @Test
    public void givenValidGameplaySessionRequestWithPlayerNotSaved_whenCreateSession_thenSessionTokenReturned() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();

        assertEquals(0, playerRepository.findAll().size());

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(gameplaySessionRequest)
                .post(BASE_URI + "/session")
                .then()
                .log().all()
                .statusCode(200)
                .extract().jsonPath();

        assertNotNull(jsonPath.get("sessionToken"));
        assertEquals(1, sessionRepository.findAll().size());
        assertEquals(1, playerRepository.findAll().size());
    }

    @Test
    public void givenValidGameplaySessionRequestWithPlayerSaved_whenCreateSession_thenSessionTokenReturned() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
        Player player = PlayerBuilder.aPlayer().build();
        playerRepository.save(player);

        assertEquals(1, playerRepository.findAll().size());

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(gameplaySessionRequest)
                .post(BASE_URI + "/session")
                .then()
                .log().all()
                .statusCode(200)
                .extract().jsonPath();

        assertNotNull(jsonPath.get("sessionToken"));
        assertEquals(1, sessionRepository.findAll().size());
        assertEquals(2, playerRepository.findAll().size());
    }

    @Test
    public void givenValidGameplaySessionRequestWithGuestPlayerSaved_whenCreateSession_thenSessionTokenReturned() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
        gameplaySessionRequest.setGuestToken(null);
        gameplaySessionRequest.setAuthToken("auth_token");
        Player player = PlayerBuilder.aPlayer().withGuest(true).build();
        playerRepository.save(player);

        assertEquals(1, playerRepository.findAll().size());

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(gameplaySessionRequest)
                .post(BASE_URI + "/session")
                .then()
                .log().all()
                .statusCode(200)
                .extract().jsonPath();

        assertNotNull(jsonPath.get("sessionToken"));
        assertEquals(1, sessionRepository.findAll().size());
        assertEquals(2, playerRepository.findAll().size());
    }

    @Test
    public void givenValidGuestGameplaySessionRequestWithGuestPlayerSaved_whenCreateSession_thenSessionTokenReturned() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
        Player player = PlayerBuilder.aPlayer().withGuest(true).build();
        playerRepository.save(player);

        assertEquals(1, playerRepository.findAll().size());

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(gameplaySessionRequest)
                .post(BASE_URI + "/session")
                .then()
                .log().all()
                .statusCode(200)
                .extract().jsonPath();

        assertNotNull(jsonPath.get("sessionToken"));
        assertEquals(1, sessionRepository.findAll().size());
        assertEquals(1, playerRepository.findAll().size());
    }

    @Test
    public void givenInvalidGameplaySessionRequest_whenCreateSession_thenThrowBadRequestException() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
        gameplaySessionRequest.setPlayerId(null);

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(gameplaySessionRequest)
                .post(BASE_URI + "/session")
                .then()
                .log().all()
                .statusCode(400)
                .extract().jsonPath();

        assertEquals("BadRequest", jsonPath.get("code"));
        assertEquals(0, sessionRepository.findAll().size());
        assertEquals(0, playerRepository.findAll().size());
    }

    @Test
    public void givenFailedRepositorySave_whenCreateSession_thenThrowInternalServerException() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();

        doThrow(new InternalServerException("Exception Message")).when(sessionRepository).save(any());

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(gameplaySessionRequest)
                .post(BASE_URI + "/session")
                .then()
                .log().all()
                .statusCode(500)
                .extract().jsonPath();

        assertEquals("InternalServer", jsonPath.get("code"));
        assertEquals(0, sessionRepository.findAll().size());
        assertEquals(0, playerRepository.findAll().size());
    }

    @Test
    public void givenFailedPlayerRepositorySave_whenCreateSession_thenThrowInternalServerException() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();

        doThrow(new InternalServerException("Exception Message2")).when(playerRepository).saveAndFlush(any());

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(gameplaySessionRequest)
                .post(BASE_URI + "/session")
                .then()
                .log().all()
                .statusCode(500)
                .extract().jsonPath();

        assertEquals("InternalServer", jsonPath.get("code"));
        assertEquals(0, sessionRepository.findAll().size());
        assertEquals(0, playerRepository.findAll().size());
    }

    @Test
    public void givenCorrectSessionToken_whenGetGameplaySessionByToken_thenReturnCorrectSession() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
        GameplaySession session = sessionService.createGameplaySession(gameplaySessionRequest);

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(BASE_URI + "/session/" + session.getSessionToken())
                .then()
                .log().all()
                .statusCode(200)
                .extract().jsonPath();

        assertEquals(session.getSessionToken(), jsonPath.get("sessionToken"));
        assertEquals(session.getId(), jsonPath.get("sessionId"));
    }

    @Test
    public void givenSessionTokenForNonexistentSession_whenGetGameplaySessionByToken_thenReturnSessionNotFound() {
        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(BASE_URI + "/session/" + "nonexistentSessionToken")
                .then()
                .log().all()
                .statusCode(404)
                .extract().jsonPath();

        assertEquals("SessionNotFound", jsonPath.get("code"));
        assertEquals("Session not found for Token: nonexistentSessionToken", jsonPath.get("msg"));
    }

    @Test
    public void givenSessionTokenForFinishedSession_whenGetGameplaySessionByToken_thenReturnInvalidState() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
        GameplaySession session = sessionService.createGameplaySession(gameplaySessionRequest);
        session.setSessionStatus(SessionStatus.FINISHED);
        sessionService.persistSession(session);

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(BASE_URI + "/session/" + session.getSessionToken())
                .then()
                .log().all()
                .statusCode(409)
                .extract().jsonPath();

        assertEquals("InvalidState", jsonPath.get("code"));
        assertEquals("Trying to access a finished session", jsonPath.get("msg"));
    }

    @Test
    public void givenFailedRepositoryRead_whenGetGameplaySessionByToken_thenReturnInternalServerError() {
        GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
        GameplaySession session = sessionService.createGameplaySession(gameplaySessionRequest);

        doThrow(new InternalServerException("Exception Message")).when(sessionRepository).findBySessionToken(any());

        JsonPath jsonPath = RestAssured.given()
                .log().all()
                .header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(BASE_URI + "/session/" + session.getSessionToken())
                .then()
                .log().all()
                .statusCode(500)
                .extract().jsonPath();

        assertEquals("InternalServer", jsonPath.get("code"));
    }
}