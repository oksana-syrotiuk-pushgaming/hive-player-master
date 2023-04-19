/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.persistence.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Converter
public class UTCDateTimeAttributeConverter 
		implements AttributeConverter<ZonedDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime utcDateTime) {
    	if (utcDateTime == null) {
    		return null;
    	}
    	if (!utcDateTime.getOffset().equals(ZoneOffset.UTC)) {
    		throw new IllegalArgumentException("not a UTC zoned datetime, offset=" +
    				utcDateTime.getOffset());
    	}
    	return Timestamp.from(utcDateTime.toInstant());
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp sqlTimestamp) {
    	return (sqlTimestamp == null ? null : sqlTimestamp.toInstant().atZone(ZoneId.of("UTC")));
    }

}
