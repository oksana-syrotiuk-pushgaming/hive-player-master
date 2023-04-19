package io.gsi.hive.platform.player.play.active;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.IgpPresets;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class ActivePlayServiceTimePeriodTests {

	private ActivePlayServiceTimePeriod activePlayServiceTimePeriod;

	@Mock
	private PlayRepository playRepository;

	@Before
	public void setup(){
		initMocks(this);
		activePlayServiceTimePeriod = new ActivePlayServiceTimePeriod(playRepository, new HashMap<>());
	}

	@Test
	public void getActivePlays_whenWithinTimePeriod_Ok() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> propertiesMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+01:00")).minusHours(1).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(1).toString();

		propertiesMap.put("iguana", after + "," + before);

		activePlayServiceTimePeriod.setCompletionTimePeriod(propertiesMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_IGUANA));
	}

	@Test
	public void getActivePlays_whenOutsideTimePeriod_Ok() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> propertiesMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(1).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(2).toString();
		propertiesMap.put("iguana", after + "," + before);

		activePlayServiceTimePeriod.setCompletionTimePeriod(propertiesMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(0);

		Mockito.verifyZeroInteractions(playRepository);
	}

	@Test
  public void getActivePlays_whenUsingTimePeriodUTC8_Ok() {
	Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
			.thenReturn(Arrays.asList(PlayBuilder.play().build()));

	Map<String,String> timePeriodMap = new HashMap<>();
	String after = OffsetTime.now(ZoneId.of("UTC+08:00")).minusHours(1).toString();
	String before = OffsetTime.now(ZoneId.of("UTC+08:00")).plusHours(1).toString();
	timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
	activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

	List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

	assertThat(plays.size()).isEqualTo(1);

	Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
			Mockito.eq(PlayStatus.ACTIVE.toString()),
			Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodUTC12_Ok() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+12:00")).minusHours(1).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+12:00")).plusHours(1).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodMaxPositiveUTC14_Ok() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+14:00")).minusHours(1).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+14:00")).plusHours(1).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodNegativeUTC6_Ok() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC-06:00")).minusHours(1).toString();
		String before = OffsetTime.now(ZoneId.of("UTC-06:00")).plusHours(1).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodNegativeUTC8_Ok() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC-08:00")).minusHours(1).toString();
		String before = OffsetTime.now(ZoneId.of("UTC-08:00")).plusHours(1).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodMaxNegativeUTC12_Ok() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC-12:00")).minusHours(1).toString();
		String before = OffsetTime.now(ZoneId.of("UTC-12:00")).plusHours(1).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodUTC6_withLargeTimePeriod() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+06:00")).minusHours(4).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+06:00")).plusHours(4).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodUTC8_withLargeTimePeriod() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+08:00")).minusHours(4).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+08:00")).plusHours(4).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodUTC12_withLargeTimePeriod() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+12:00")).minusHours(4).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+12:00")).plusHours(4).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodNegativeUTC6_withLargeTimePeriod() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC-06:00")).minusHours(4).toString();
		String before = OffsetTime.now(ZoneId.of("UTC-06:00")).plusHours(4).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodNegativeUTC8_withLargeTimePeriod() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC-08:00")).minusHours(4).toString();
		String before = OffsetTime.now(ZoneId.of("UTC-08:00")).plusHours(4).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodMaxPositiveUTC14_withLargeTimePeriod() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC+14:00")).minusHours(4).toString();
		String before = OffsetTime.now(ZoneId.of("UTC+14:00")).plusHours(4).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}

	@Test
	public void getActivePlays_whenUsingTimePeriodMaxNegativeUTC12_withLargeTimePeriod() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
				.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,String> timePeriodMap = new HashMap<>();
		String after = OffsetTime.now(ZoneId.of("UTC-12:00")).minusHours(4).toString();
		String before = OffsetTime.now(ZoneId.of("UTC-12:00")).plusHours(4).toString();
		timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
		activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

		List<Play> plays = activePlayServiceTimePeriod.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);

		Mockito.verify(playRepository).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_GECKO));
	}
}


