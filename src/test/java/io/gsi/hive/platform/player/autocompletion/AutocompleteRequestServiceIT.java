package io.gsi.hive.platform.player.autocompletion;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.game.GameBuilder;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.mapper.MapperConfig;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.play.PlayService;
import io.gsi.hive.platform.player.presets.AutocompleteRequestPresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SuppressWarnings("NewClassNamingConvention")
public class AutocompleteRequestServiceIT extends ApiITBase {

    private MockRestServiceServer mockRestServiceServer;
    @Autowired private AutocompleteRequestService autocompleteRequestService;
    @Autowired @Qualifier("autocompleteRestTemplate") private RestTemplate restTemplate;
    @MockBean private AutocompleteRequestRepository autocompleteRequestRepository;
    @MockBean private DiscoveryClient discoveryClient;
    @MockBean private GameService gameService;
    @MockBean private PlayService playService;
    @Autowired private CacheManager cacheManager;

    private AutocompleteRequest autocompleteRequest;

    @Before
    public void setup() {
        this.mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);

        autocompleteRequest = AutocompleteRequest.builder()
        		.createdAt(Instant.now().atZone(MapperConfig.UTC_ZONE))
                .retries(0)
                .playId(AutocompleteRequestPresets.PLAYID)
                .gameCode(AutocompleteRequestPresets.GAMECODE)
                .sessionId(SessionPresets.SESSIONID)
                .guest(false)
                .build();

        AutocompleteRequest secondaryAutocompleteRequest = AutocompleteRequest.builder().playId(AutocompleteRequestPresets.SECONDARY_PLAYID).gameCode(AutocompleteRequestPresets.SECONDARY_GAMECODE)
                .createdAt(Instant.now().atZone(MapperConfig.UTC_ZONE))
                .retries(0)
                .playId(AutocompleteRequestPresets.SECONDARY_PLAYID)
                .gameCode(AutocompleteRequestPresets.GAMECODE)
                .sessionId("testSession2")
                .guest(true)
                .build();

        Mockito.when(autocompleteRequestRepository.findAndLockByPlayId(AutocompleteRequestPresets.PLAYID))
                .thenReturn(autocompleteRequest);

        Mockito.when(autocompleteRequestRepository.findAndLockByPlayId(AutocompleteRequestPresets.SECONDARY_PLAYID))
                .thenReturn(secondaryAutocompleteRequest);

        Mockito.when(gameService.getGame(AutocompleteRequestPresets.GAMECODE)).thenReturn(
                GameBuilder.aGame().withCode(AutocompleteRequestPresets.GAMECODE).build());

        Mockito.when(gameService.getGame(AutocompleteRequestPresets.SECONDARY_GAMECODE)).thenReturn(
                GameBuilder.aGame().withCode(AutocompleteRequestPresets.SECONDARY_GAMECODE).build());

        Mockito.when(discoveryClient.getServices()).thenReturn(
                Arrays.asList(AutocompleteRequestPresets.SERVICENAME, AutocompleteRequestPresets.SECONDARY_SERVICENAME));
    }

    @Before @After
    public void clearCache() {
        cacheManager.getCache("gameCache").clear();
    }

    @Test
    public void givenOkRequest_whenSendRequestIsCalled_routesRequestToExpectedService() {
        mockRestServiceServer.expect(requestTo("http://hive-game-testGame-service/hive/s2s/play/1001-2/autocomplete"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$.playId").value(AutocompleteRequestPresets.SECONDARY_PLAYID))
                .andExpect(jsonPath("$.sessionId").value("testSession2"))
                .andExpect(jsonPath("$.gameCode").value(AutocompleteRequestPresets.GAMECODE))
                .andExpect(jsonPath("$.guest").value(true))
                .andRespond(withSuccess());

        autocompleteRequestService.sendRequest(AutocompleteRequestPresets.SECONDARY_PLAYID);

        mockRestServiceServer.verify();

        Mockito.verify(autocompleteRequestRepository).deleteById(AutocompleteRequestPresets.SECONDARY_PLAYID);
        Mockito.verify(playService).markPlayAsAutocompleted(AutocompleteRequestPresets.SECONDARY_PLAYID);
    }

    @Test
    public void givenGameNotFoundRequest_whenSendRequestIsCalled_incrementsRetriesAndSetsExceptionWithoutRemovingFromQueue() {
        //given
        String gameCode = "reskin";

        AutocompleteRequest autocompleteRequest = AutocompleteRequest.builder().gameCode(gameCode).retries(1).build();

        AutocompleteRequest incrementedAutocompleteRequest =
            AutocompleteRequest.builder().gameCode(gameCode).createdAt(autocompleteRequest.getCreatedAt())
                .exception("InvalidStateException").retries(2).build();

        Mockito.when(gameService.getGame(anyString())).thenReturn(
                GameBuilder.aGame().withCode(gameCode)
                        .withServiceCode("notFound").build());

        Mockito.when(autocompleteRequestRepository.findAndLockByPlayId(any()))
                .thenReturn(autocompleteRequest);

        Mockito.when(discoveryClient.getServices()).thenReturn(Collections.singletonList(AutocompleteRequestPresets.SERVICENAME));

        //when
        autocompleteRequestService.sendRequest(autocompleteRequest.getPlayId());

        //then
        Mockito.verify(autocompleteRequestRepository, never()).deleteById(anyString());

        Mockito.verify(autocompleteRequestRepository).save(
                incrementedAutocompleteRequest);

        Mockito.verify(playService, never()).markPlayAsAutocompleted(any());
    }

    @Test
    public void givenOkRequest_whenSendRequestIsCalled_sendsRequestThenRemovesFromQueue() {
        mockRestServiceServer.expect(requestTo("http://hive-game-testGame-service/hive/s2s/play/1001-1/autocomplete"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(jsonPath("$.playId").value(AutocompleteRequestPresets.PLAYID))
            .andExpect(jsonPath("$.sessionId").value(SessionPresets.SESSIONID))
            .andExpect(jsonPath("$.gameCode").value(AutocompleteRequestPresets.GAMECODE))
            .andExpect(jsonPath("$.guest").value(false))
            .andRespond(withSuccess());

        autocompleteRequestService.sendRequest(AutocompleteRequestPresets.PLAYID);

        mockRestServiceServer.verify();

        Mockito.verify(autocompleteRequestRepository).deleteById(AutocompleteRequestPresets.PLAYID);
        Mockito.verify(playService).markPlayAsAutocompleted(AutocompleteRequestPresets.PLAYID);
    }

    @Test
    public void givenServerErrorRequest_whenSendRequestIsCalled_incrementsRetriesAndSetsExceptionWithoutRemovingFromQueue() {
        mockRestServiceServer.expect(requestTo("http://hive-game-testGame-service/hive/s2s/play/1001-1/autocomplete"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(jsonPath("$.playId").value(AutocompleteRequestPresets.PLAYID))
            .andExpect(jsonPath("$.sessionId").value(SessionPresets.SESSIONID))
            .andExpect(jsonPath("$.gameCode").value(AutocompleteRequestPresets.GAMECODE))
            .andExpect(jsonPath("$.guest").value(false))
            .andRespond(withServerError());

        AutocompleteRequest incrementedAutocompleteRequest = AutocompleteRequest.builder()
            .createdAt(autocompleteRequest.getCreatedAt())
            .retries(1)
            .playId(AutocompleteRequestPresets.PLAYID)
            .gameCode(AutocompleteRequestPresets.GAMECODE)
            .sessionId(SessionPresets.SESSIONID)
            .guest(false)
            .exception("InternalServerError")
            .build();

        autocompleteRequestService.sendRequest(autocompleteRequest.getPlayId());

        Mockito.verify(autocompleteRequestRepository).save(incrementedAutocompleteRequest);
        Mockito.verify(autocompleteRequestRepository, Mockito.times(0)).deleteById(anyString());
        Mockito.verify(playService, never()).markPlayAsAutocompleted(any());
    }
}
