/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.txn.info;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.hive.platform.player.mapper.MapperConfig;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.Map;

@Converter
public class ExtraInfoConverter implements AttributeConverter<Map<String, Object>, String> {
	private ObjectMapper objectMapper;
	private JavaType objectType;

	public ExtraInfoConverter() {
		objectMapper = new MapperConfig().objectMapper();
		objectType = objectMapper.getTypeFactory().constructType(Map.class);
	}

	@Override
	public String convertToDatabaseColumn(Map<String, Object> extraInfo) {
		if (extraInfo == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(extraInfo);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("failed to convert to json", e);
		}
	}

	@Override
	public Map<String, Object> convertToEntityAttribute(String extraInfoJson) {
		if (extraInfoJson == null || extraInfoJson.length() == 0) {
			return null;
		}
		try {
			return objectMapper.readValue(extraInfoJson, objectType);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to map json to object", e);
		}
	}
}
