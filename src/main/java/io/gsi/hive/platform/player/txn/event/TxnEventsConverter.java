/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.txn.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.gsi.hive.platform.player.mapper.MapperConfig;

public class TxnEventsConverter implements AttributeConverter<List<TxnEvent>,String> {
	private ObjectMapper objectMapper;
	private JavaType txnEventType;

	public TxnEventsConverter() {
		objectMapper = new MapperConfig().objectMapper();
		txnEventType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class,TxnEvent.class);
	}

	@Override
	public String convertToDatabaseColumn(List<TxnEvent> txnEvents) {
		if (txnEvents == null || txnEvents.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(txnEvents);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("failed to convert to json",e);
		}
	}

	@Override
	public List<TxnEvent> convertToEntityAttribute(String txnEventsJson) {
		if (txnEventsJson == null || txnEventsJson.length() == 0) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(txnEventsJson, txnEventType);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to map json to object",e);
		}
	}
}
