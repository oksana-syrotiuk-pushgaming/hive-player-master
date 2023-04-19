package io.gsi.hive.platform.player.play.active;

import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.util.CalendarConverter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hive.autocomplete.algorithm", havingValue = "legacy")
public class ActivePlayServiceDeadlineMinutes implements ActivePlayService {

	private final PlayRepository playRepository;
	private Map<String, Integer> completionDeadlineMinutes;

	public void setCompletionDeadlineMinutes(Map<String, Integer> completionDeadlineMinutes) {
		this.completionDeadlineMinutes = completionDeadlineMinutes;
	}

	public ActivePlayServiceDeadlineMinutes(PlayRepository playRepository,
											@Value("#{${hive.autocomplete.completionDeadlineMinutes:{:}}}")
													Map<String, Integer> completionDeadlineMinutes) {
		this.playRepository = playRepository;
		this.setCompletionDeadlineMinutes(completionDeadlineMinutes);
	}

	@Override
	public Stream<Play> getActivePlays() {
		List<Play> activePlays = new ArrayList<>();
		completionDeadlineMinutes.forEach((String igpCode, Integer ageMinutes) ->
				activePlays.addAll(
						this.playRepository.findAllByStatusAndIgpCodeAndCreatedAtBefore(
								PlayStatus.ACTIVE.name(),
								igpCode,
								CalendarConverter.convertToCalendar(ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(ageMinutes)))));

		return activePlays.stream();
	}
}
