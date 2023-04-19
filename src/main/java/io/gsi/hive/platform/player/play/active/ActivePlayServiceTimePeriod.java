package io.gsi.hive.platform.player.play.active;


import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hive.autocomplete.algorithm", havingValue = "legacy")
public class ActivePlayServiceTimePeriod implements ActivePlayService {

	private final PlayRepository playRepository;
	private Map<String, List<LocalTime>> completionTimePeriod;

	public void setCompletionTimePeriod(Map<String, String> completionTimePeriod) {
		this.completionTimePeriod = new HashMap<>();
		for (String key : completionTimePeriod.keySet()) {
			this.completionTimePeriod.put(key, convertToLocalTime(splitStringIntoOffsetTimes(completionTimePeriod.get(key))));
		}
	}

	@Autowired
	public ActivePlayServiceTimePeriod(PlayRepository playRepository, @Value("#{${hive.autocomplete.completionTimePeriod:{:}}}") Map<String, String> completionTimePeriod) {
		this.playRepository = playRepository;
		this.setCompletionTimePeriod(completionTimePeriod);
	}

	/**
	 * With an input of startTime 23:00, endTime 03:00.
	 * OffsetTime is compared on a common day, so 03:00 becomes the morning before 23:00.
	 * e.g.
	 * Now = 23:30
	 * if(after 23:00)this is true
	 * if(before 03:00)this is false
	 *
	 * Solution:
	 * 1 = timeStart, 2 = timeEnd
	 * if (2 > 1)
	 *  is (now > 1 && now < 2)
	 * else if (1 > 2)
	 *  is (now > 1 || now < 2)
	 *
	 * Issues with wrapping of days due to time zone differences
	 * have been resolved by converting to localTime before comparing, see convertToLocalTime() below.
	 * @return
	 */
	@Override
	public Stream<Play> getActivePlays() {
		List<Play> activePlays = new ArrayList<>();

		for (String igpCode : completionTimePeriod.keySet()) {

			LocalTime nowOffsetTime = LocalTime.now(ZoneId.of("UTC"));
			LocalTime startTime = completionTimePeriod.get(igpCode).get(0);
			LocalTime endTime = completionTimePeriod.get(igpCode).get(1);

			if (isEligibleForAutoCompletion(nowOffsetTime, startTime, endTime)) {
				activePlays.addAll(
						this.playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
								PlayStatus.ACTIVE.name(),
								igpCode));
			}
		}
		return activePlays.stream();
	}

	public static boolean isEligibleForAutoCompletion(LocalTime nowOffsetTime, LocalTime startTime, LocalTime endTime){
		if (endTime.isAfter(startTime)) {
			if (nowOffsetTime.isAfter(startTime) &&
					nowOffsetTime.isBefore(endTime)) {
				return true;
			}
		} else if (nowOffsetTime.isAfter(startTime) ||
				nowOffsetTime.isBefore(endTime)) {
			return true;
		}
		return false;
	}

	public static List<OffsetTime> splitStringIntoOffsetTimes(String offsets) {
		String[] splitOffsets = offsets.split(",");
		if (splitOffsets.length != 2) {
			throw new InvalidStateException(
					"AutoCompletion could not find both an after and before timestamp");
		}
		return Arrays.asList(OffsetTime.parse(splitOffsets[0]), OffsetTime.parse(splitOffsets[1]));
	}

	/**
	 * Used to convert OffsetTime to LocalTime in UTC, doing so avoids issues with
	 * wrapping of days based on time zones.
	 * This handles both +UTC values and -UTC values.
	 * @param offsetTimes
	 * @return
	 */
	public static List<LocalTime> convertToLocalTime(List<OffsetTime> offsetTimes) {
		LocalTime after = offsetTimes.get(0).minusSeconds(offsetTimes.get(0).getOffset().getTotalSeconds()).toLocalTime();
		LocalTime before = offsetTimes.get(1).minusSeconds(offsetTimes.get(1).getOffset().getTotalSeconds()).toLocalTime();

		return Arrays.asList(after, before);
	}
}
