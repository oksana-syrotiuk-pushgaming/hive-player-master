package io.gsi.hive.platform.player.cache;

import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

public class ParameterRetrievingKey implements Serializable {
    public static final SimpleKey EMPTY = new SimpleKey();
    private final Object[] params;
    private final int hashCode;

    public ParameterRetrievingKey(Object... elements) {
        Assert.notNull(elements, "Elements must not be null");
        this.params = new Object[elements.length];
        System.arraycopy(elements, 0, this.params, 0, elements.length);
        this.hashCode = Arrays.deepHashCode(this.params);
    }

    public Object[] getParams() {
        return params;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other ||
                (other instanceof ParameterRetrievingKey && Arrays.deepEquals(this.params, ((ParameterRetrievingKey) other).params)));
    }

    @Override
    public final int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
    }
}
