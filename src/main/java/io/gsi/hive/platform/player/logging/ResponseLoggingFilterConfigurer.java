package io.gsi.hive.platform.player.logging;

import io.gsi.commons.http.response.ResponseLoggingFilter;

public interface ResponseLoggingFilterConfigurer {

	public void configure(ResponseLoggingFilter filter);

}
