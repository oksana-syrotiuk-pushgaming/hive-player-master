package io.gsi.hive.platform.player.play.active;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.IgpPresets;

public class ActivePlayServiceDeadlineMinutesTests{

	private ActivePlayServiceDeadlineMinutes activePlayServiceDeadlineMinutes;

	@Mock
	private PlayRepository playRepository;

	@Before
	public void setup(){
		initMocks(this);
	}

	@Test
	public void testGetActivePlaysOk() {
		Mockito.when(playRepository.findAllByStatusAndIgpCodeAndCreatedAtBefore(any(), any(), any()))
		.thenReturn(Arrays.asList(PlayBuilder.play().build()));

		Map<String,Integer> propertiesMap = new HashMap<>();
		propertiesMap.put(IgpPresets.IGPCODE_IGUANA, 60);
		activePlayServiceDeadlineMinutes = new ActivePlayServiceDeadlineMinutes(playRepository, propertiesMap);

		List<Play> plays = activePlayServiceDeadlineMinutes.getActivePlays().collect(Collectors.toList());

		assertThat(plays.size()).isEqualTo(1);
				
		Mockito.verify(playRepository).findAllByStatusAndIgpCodeAndCreatedAtBefore(
				Mockito.eq(PlayStatus.ACTIVE.toString()),
				Mockito.eq(IgpPresets.IGPCODE_IGUANA),
				Mockito.argThat(new isCalendarWithin(2, Calendar.HOUR, Calendar.getInstance())));
	}

	private class isCalendarWithin implements ArgumentMatcher<Calendar> {

		private int unit;
		private Calendar earlier;
		private Calendar later;
		
		public isCalendarWithin(int within, int units, Calendar of) {
			super();
			this.unit = units;
			this.earlier = (Calendar) of.clone();
			this.later = (Calendar) of.clone();
			this.earlier.add(unit, -within);
			this.later.add(unit, within);
		}

		@Override
		public boolean matches(Calendar calendar) {
			return calendar.compareTo(earlier) > 0 && calendar.compareTo(later) < 0;
		}
	}
}


