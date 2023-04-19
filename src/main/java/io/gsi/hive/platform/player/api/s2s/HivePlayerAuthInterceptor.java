/**
 * © gsi.io 2014
 */

package io.gsi.hive.platform.player.api.s2s;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.logging.LogLevel;
import io.gsi.commons.logging.Loggable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HivePlayerAuthInterceptor
 *
 * Implements basic API key checking for the external APIs
 */
@Component
@Loggable(logLevel=LogLevel.DEBUG)
public class HivePlayerAuthInterceptor extends HandlerInterceptorAdapter {

	private static final Log logger = LogFactory.getLog(HivePlayerAuthInterceptor.class);

	public static final String API_KEY_NAME = "Hive-Player-API-Key";

	private final String apiKey;
	private boolean apiKeyEnabled;

	public HivePlayerAuthInterceptor(@Value("${hive.player.apiKey}") String apiKey,
									 @Value("${hive.player.apiKey.enabled:true}") boolean apiKeyEnabled) {
		this.apiKey = apiKey;
		setApiKeyEnabled(apiKeyEnabled);
	}

	public void setApiKeyEnabled(boolean apiKeyEnabled) {
		this.apiKeyEnabled = apiKeyEnabled;
	}

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		//Only perform key check when enabled
		if(apiKeyEnabled) {

			if (apiKey == null || apiKey.length() == 0) {
				throw new InternalServerException("No API key configured");
			}
			String suppliedApiKey = request.getHeader(API_KEY_NAME);
			if (suppliedApiKey == null || suppliedApiKey.length() == 0) {
				logger.error("No Hive API Key supplied in header");
				throw new AuthorizationException("No Hive API Key supplied in header");
			}
			if (!suppliedApiKey.contentEquals(apiKey)) {
				logger.error(String.format(
						"Invalid Hive API Key supplied in header: %s",suppliedApiKey));
				throw new AuthorizationException("Invalid Hive API Key");
			}
		}
		return true;
	}
}
