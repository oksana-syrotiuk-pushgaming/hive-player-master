package io.gsi.hive.platform.player.api.bo;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.InvalidStateException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@ConditionalOnProperty(value="hive.player.backOffice.apiKey.enabled",
    havingValue = "true")
@Slf4j
@Component
public class BoApiKeyInterceptor extends HandlerInterceptorAdapter {
    public static final String API_KEY_NAME = "Hive-Player-Back-Office-API-Key";
    private String apiKey;

    BoApiKeyInterceptor(@Value("${hive.player.backOffice.apiKey}") String apiKey) {
        if (apiKey == null || apiKey.length() == 0) {
            throw new InvalidStateException("No Back Office API key configured");
        }
        this.apiKey = apiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
        HttpServletResponse response, Object handler) throws Exception {
        String suppliedApiKey = request.getHeader(API_KEY_NAME);
        if (!apiKey.equals(suppliedApiKey)) {
            log.error("Invalid Hive API Key supplied in header");
            throw new AuthorizationException("Invalid BO API Key");
        }
        return true;
    }
}
