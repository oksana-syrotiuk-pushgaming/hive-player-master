/**
 * © gsi.io 2015
 */
package io.gsi.hive.platform.player.validation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Validator;

/**
 * ValidationConfig
 *
 */
@Configuration
public class ValidationConfig {

	@Bean
	public Validator validator() {
		return new LocalValidatorFactoryBean();
	}

}
