/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mapper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.gsi.commons.util.JsonMapper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

	public static final DateFormat APP_DATE_FORMAT = 
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public static final ZoneId UTC_ZONE = ZoneId.of("UTC");
	
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_ABSENT);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
		objectMapper.setDateFormat(APP_DATE_FORMAT);
		objectMapper.registerModule(new JavaTimeModule());
		//Disabled for empty extra info objects
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		return objectMapper;
	}

	@Bean
	public JsonMapper jsonMapper() {
		JsonMapper jsonMapper = new JsonMapper();
		jsonMapper.setObjectMapper(objectMapper());
		return jsonMapper;
	}

}
