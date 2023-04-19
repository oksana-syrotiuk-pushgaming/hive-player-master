package io.gsi.hive.platform.player.play.active;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.autocompletion.AutocompleteQueueingScheduledService;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@TestPropertySource(properties={
        "hive.autocomplete.completionTimePeriod={norsktipping:'03:00:00+01:00,06:00:00+01:00'}",
        "hive.autocomplete.completionDeadlineMinutes={override:5}",
        "hive.autocomplete.defaultCompletionDeadlineMinutes:2880",
        "hive.autocomplete.algorithm=default",
        "hive.session.expirySecs=300"})
@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL})
public class DefaultActivePlayServiceIT extends PersistenceITBase {

  @SpyBean private DefaultActivePlayService defaultActivePlayService;
  @Autowired private AutocompleteQueueingScheduledService autocompleteQueueingScheduledService;
  @Autowired private AutocompleteRequestRepository autocompleteRequestRepository;
  @MockBean
  private PlayRepository playRepository;
  @MockBean
  private TxnRepository txnRepository;


  @Value("${hive.autocomplete.defaultCompletionDeadlineMinutes}")
  private Integer defaultCompletionDeadlineMinutes;

  @Value("${hive.session.expirySecs}")
  private Integer sessionExpirySecs;

  @Before
  public void setup(){
    when(txnRepository.findByPlayIdAndTypeIn(anyString(), any())).thenReturn(List.of(
        TxnBuilder.txn()
            .withTxnId("transaction_1")
            .withSessionId(SessionPresets.SESSIONID)
            .withType(TxnType.STAKE)
            .build())
    );
  }

  @Test
  public void givenPlayDuringAutocompletionPeriod_whenGetActivePlaysAndQueueAutocompletion_thenPlayQueued() {
    final var now = ZonedDateTime.parse("2021-08-17T03:00:01+01:00")
        .withZoneSameInstant(ZoneId.of("UTC"));

    when(defaultActivePlayService.getTimeStamp()).thenReturn(now);

    when(playRepository.findAllActive()).thenReturn(Stream.of(
        PlayBuilder.play()
            .withIgpCode("norsktipping")
            .withStatus(PlayStatus.ACTIVE).build()
    ));

    autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

    final var queuedRequests = autocompleteRequestRepository.findAll();

    assertThat(queuedRequests.size()).isEqualTo(1);
    assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
    assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);

    Mockito.reset(defaultActivePlayService);
  }


  @Test
  public void givenPlayPastDeadline_whenGetActivePlaysAndQueueAutocompletion_thenPlayQueued() {
    when(playRepository.findAllActive()).thenReturn(Stream.of(
        PlayBuilder.play()
            .withStatus(PlayStatus.ACTIVE)
            .withCreatedAt(Instant.now()
                               .atZone(ZoneId.of("UTC"))
                               .minusMinutes(defaultCompletionDeadlineMinutes + 1)
            ).build(),
        PlayBuilder.play()
            .withPlayId("outOfRange")
            .withStatus(PlayStatus.ACTIVE)
            .withCreatedAt(Instant.now().atZone(ZoneId.of("UTC")))
            .build()
    ));

    autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

    final var queuedRequests = autocompleteRequestRepository.findAll();

    assertThat(queuedRequests.size()).isEqualTo(1);
    assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
    assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);
  }

  @Test
  public void givenPlayPastCustomDeadline_whenGetActivePlaysAndQueueAutocompletion_thenPlayQueued() {
    when(playRepository.findAllActive()).thenReturn(Stream.of(
        PlayBuilder.play()
            .withIgpCode("override")
            .withStatus(PlayStatus.ACTIVE)
            .withCreatedAt(Instant.now()
                               .atZone(ZoneId.of("UTC"))
                               .minusMinutes(5 + 1)
            ).build(),
        PlayBuilder.play()
            .withIgpCode("override")
            .withPlayId("outOfRange")
            .withStatus(PlayStatus.ACTIVE)
            .withCreatedAt(Instant.now()
                               .atZone(ZoneId.of("UTC"))
            ).build()
    ));

    autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

    final var queuedRequests = autocompleteRequestRepository.findAll();

    assertThat(queuedRequests.size()).isEqualTo(1);
    assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
    assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);
  }

  @Test
  public void givenGuestDemoPlayPastTwiceSessionExpiry_whenGetActivePlaysAndQueueAutocompletion_thenPlayQueued() {
    long testPlayAge = 2L * sessionExpirySecs + 1;
    Play testPlay = PlayBuilder.play()
            .withIgpCode(IgpPresets.IGPCODE_IGUANA)
            .withStatus(PlayStatus.ACTIVE)
            .withGuest(true)
            .withMode(Mode.demo)
            .withCreatedAt(Instant.now()
                    .atZone(ZoneId.of("UTC"))
                    .minusSeconds(testPlayAge)).build();

    when(playRepository.findAllActive()).thenReturn(Stream.of(testPlay));

    autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

    final var queuedRequests = autocompleteRequestRepository.findAll();

    assertThat(queuedRequests.size()).isEqualTo(1);
    assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(testPlay.getPlayId());
  }

  @Test
  public void givenGuestDemoPlayPastTwiceSessionExpiry_whenGetActivePlays_thenGuestDemoPlayIncluded() {
    long testPlayAge = 2L * sessionExpirySecs + 1;
    Play testPlay = PlayBuilder.play()
            .withIgpCode(IgpPresets.IGPCODE_IGUANA)
            .withStatus(PlayStatus.ACTIVE)
            .withGuest(true)
            .withMode(Mode.demo)
            .withCreatedAt(Instant.now()
                    .atZone(ZoneId.of("UTC"))
                    .minusSeconds(testPlayAge)).build();

    when(playRepository.findAllActive()).thenReturn(Stream.of(testPlay));

    final var activePlays = defaultActivePlayService.getActivePlays();

    assertThat(activePlays).containsOnly(testPlay);
  }

  @Test
  public void givenGuestDemoPlayUnderSessionExpiry_whenGetActivePlays_thenGuestDemoPlayExcluded() {
    long testPlayAge = sessionExpirySecs - 1;
    Play testPlay = PlayBuilder.play()
            .withIgpCode(IgpPresets.IGPCODE_IGUANA)
            .withStatus(PlayStatus.ACTIVE)
            .withGuest(true)
            .withMode(Mode.demo)
            .withCreatedAt(Instant.now()
                    .atZone(ZoneId.of("UTC"))
                    .minusSeconds(testPlayAge)).build();

    when(playRepository.findAllActive()).thenReturn(Stream.of(testPlay));

    final var activePlays = defaultActivePlayService.getActivePlays();

    assertThat(activePlays).isEmpty();
  }

  @Test
  public void givenMultipleGuestDemoPlays_whenGetActivePlays_thenOnlyPlaysOlderThanTwiceSessionExpiryIncluded() {
    long expectedPlayAge = 2L * sessionExpirySecs + 1;
    long unexpectedPlayAge = sessionExpirySecs;
    Play expectedPlay = PlayBuilder.play()
            .withIgpCode(IgpPresets.IGPCODE_IGUANA)
            .withStatus(PlayStatus.ACTIVE)
            .withGuest(true)
            .withMode(Mode.demo)
            .withCreatedAt(Instant.now()
                    .atZone(ZoneId.of("UTC"))
                    .minusSeconds(expectedPlayAge)).build();
    Play unexpectedPlay = PlayBuilder.play()
            .withIgpCode(IgpPresets.IGPCODE_IGUANA)
            .withStatus(PlayStatus.ACTIVE)
            .withGuest(true)
            .withMode(Mode.demo)
            .withCreatedAt(Instant.now()
                    .atZone(ZoneId.of("UTC"))
                    .minusSeconds(unexpectedPlayAge)).build();

    when(playRepository.findAllActive()).thenReturn(Stream.of(expectedPlay, unexpectedPlay));

    final var activePlays = defaultActivePlayService.getActivePlays();

    assertThat(activePlays)
            .containsOnly(expectedPlay)
            .doesNotContain(unexpectedPlay);
  }

  @Test
  public void givenLoggedInDemoPlaysPastTwiceSessionExpiry_whenGetActivePlays_thenLoggedInDemoPlayExcluded() {
    long testPlayAge = 2L * sessionExpirySecs + 1;
    Play testPlay = PlayBuilder.play()
            .withIgpCode(IgpPresets.IGPCODE_IGUANA)
            .withStatus(PlayStatus.ACTIVE)
            .withGuest(false)
            .withMode(Mode.demo)
            .withCreatedAt(Instant.now()
                    .atZone(ZoneId.of("UTC"))
                    .minusSeconds(testPlayAge)).build();

    when(playRepository.findAllActive()).thenReturn(Stream.of(testPlay));

    final var activePlays = defaultActivePlayService.getActivePlays();

    assertThat(activePlays).isEmpty();
  }
}