package io.gsi.hive.platform.player.util;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

public class CalendarConverter {

  public static Calendar convertToCalendar(ZonedDateTime timeStamp) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timeStamp.toInstant().toEpochMilli());
    calendar.setTimeZone(TimeZone.getTimeZone(timeStamp.getZone()));
    return calendar;
  }

}
