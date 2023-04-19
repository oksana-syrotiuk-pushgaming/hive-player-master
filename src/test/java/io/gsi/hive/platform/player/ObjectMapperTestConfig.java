/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.validation.Validator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.gsi.commons.util.JsonMapper;

@Configuration
public class ObjectMapperTestConfig
{

	public static final DateFormat APP_DATE_FORMAT =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Primary
	@Bean
	public ObjectMapper commonObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.configure(
				SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
		objectMapper.setDateFormat(APP_DATE_FORMAT);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		return objectMapper;
	}

	@Bean
	public JsonMapper commonJsonMapper() {
		JsonMapper jsonMapper = new JsonMapper();
		jsonMapper.setObjectMapper(commonObjectMapper());
		return jsonMapper;
	}

	@Bean
	public Validator validator() {
		return new LocalValidatorFactoryBean();
	}

}
