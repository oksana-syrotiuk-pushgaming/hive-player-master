package io.gsi.hive.platform.player.play.active;

import static io.gsi.hive.platform.player.play.active.ActivePlayServiceTimePeriod.convertToLocalTime;
import static io.gsi.hive.platform.player.play.active.ActivePlayServiceTimePeriod.splitStringIntoOffsetTimes;

import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayRepository;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hive.autocomplete.algorithm", havingValue = "default", matchIfMissing = true)
public class DefaultActivePlayService implements ActivePlayService {

	private final PlayRepository playRepository;
	private final SessionService sessionService;
	private final Map<String, Integer> completionDeadlineMinutes;
	private final Map<String, List<LocalTime>> completionTimePeriod;
	private final Integer defaultCompletionDeadlineMinutes;

	public DefaultActivePlayService(PlayRepository playRepository,
									@Value("#{${hive.autocomplete.completionDeadlineMinutes:{:}}}")
											Map<String, Integer> completionDeadlineMinutes,
									@Value("#{${hive.autocomplete.completionTimePeriod:{:}}}")
											Map<String, String> completionTimePeriod,
									@Value("${hive.autocomplete.defaultCompletionDeadlineMinutes:2880}")
											Integer defaultCompletionDeadlineMinutes,
									SessionService sessionService) {
		this.playRepository = playRepository;
		this.sessionService = sessionService;
		this.completionDeadlineMinutes = completionDeadlineMinutes;
		this.defaultCompletionDeadlineMinutes = defaultCompletionDeadlineMinutes;
		this.completionTimePeriod = new HashMap<>();
		for (String key : completionTimePeriod.keySet()) {
			this.completionTimePeriod.put(key, convertToLocalTime(splitStringIntoOffsetTimes(completionTimePeriod.get(key))));
		}
	}

	public boolean isEligibleForAutoCompletionByTimePeriod(Play play, ZonedDateTime now){
		final var igpCode = play.getIgpCode();
		LocalTime nowOffsetTime = now.toLocalTime();
		LocalTime startTime = completionTimePeriod.get(igpCode).get(0);
		LocalTime endTime = completionTimePeriod.get(igpCode).get(1);

		return ActivePlayServiceTimePeriod.isEligibleForAutoCompletion(nowOffsetTime, startTime, endTime);
	}

	private boolean isEligibleForAutoCompletionByDeadlineMinutes(Play play, ZonedDateTime now){
		final var ageMinutes = completionDeadlineMinutes.getOrDefault(play.getIgpCode(), defaultCompletionDeadlineMinutes);
		final var latestAllowed = now.minusMinutes(ageMinutes);
		return play.getCreatedAt().isBefore(latestAllowed);
	}

	private boolean isEligibleForAutoCompletionBySession(Play play, ZonedDateTime now) {
		long secondsSincePlay = Duration.between(play.getCreatedAt(), now).getSeconds();
		return secondsSincePlay > (2L * sessionService.getSessionExpirySecs());
	}

	private boolean isEligibleForAutoCompletion(Play play, ZonedDateTime now){
    boolean guestDemoPlayAutoCompletable = play.isGuest() && play.getMode() == Mode.demo
			&& isEligibleForAutoCompletionBySession(play, now);
	return guestDemoPlayAutoCompletable ||
			(completionTimePeriod.containsKey(play.getIgpCode()) ?
					isEligibleForAutoCompletionByTimePeriod(play, now)
					: isEligibleForAutoCompletionByDeadlineMinutes(play, now));
	}

	protected ZonedDateTime getTimeStamp(){
		return ZonedDateTime.now(ZoneId.of("UTC"));
	}

	@Override
	public Stream<Play> getActivePlays() {
		final var now = getTimeStamp();
		return playRepository.findAllActive()
				.filter(play -> isEligibleForAutoCompletion(play, now));
	}
}
