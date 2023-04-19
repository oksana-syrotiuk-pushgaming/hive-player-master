package io.gsi.hive.platform.player.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

public class ParameterRetrievingKeyGenerator implements KeyGenerator {
    @Override
    public @NonNull Object generate(Object target, Method method, @NonNull Object... params) {
        return new ParameterRetrievingKey(target.getClass(), method.getName(), params);
    }
}
