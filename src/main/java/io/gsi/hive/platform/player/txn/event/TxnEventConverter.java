/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.txn.event;

import java.io.IOException;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.gsi.hive.platform.player.mapper.MapperConfig;

public class TxnEventConverter implements AttributeConverter<TxnEvent,String> {
	private ObjectMapper objectMapper;

	public TxnEventConverter() {
		objectMapper = new MapperConfig().objectMapper();
	}

	@Override
	public String convertToDatabaseColumn(TxnEvent event) {
		if (event == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("failed to convert to json",e);
		}
	}

	@Override
	public TxnEvent convertToEntityAttribute(String eventJson) {
		if (eventJson == null || eventJson.length() == 0) {
			return null;
		}
		try {
			return objectMapper.readValue(eventJson,TxnEvent.class);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to map json to object",e);
		}
	}
}
