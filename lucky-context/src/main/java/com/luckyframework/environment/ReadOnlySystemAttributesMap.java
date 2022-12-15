package com.luckyframework.environment;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Read-only {@code Map<String, String>} implementation that is backed by system
 * properties or environment variables.
 *
 * <p>Used by {@link AbstractEnvironment} when a {@link SecurityManager} prohibits
 * access to {@link System#getProperties()} or {@link System#getenv()}. It is for this
 * reason that the implementations of {@link #keySet()}, {@link #entrySet()}, and
 * {@link #values()} always return empty even though {@link #get(Object)} may in fact
 * return non-null if the current security manager allows access to individual keys.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.0
 */
abstract class ReadOnlySystemAttributesMap implements Map<String, String> {

    @Override
    public boolean containsKey(Object key) {
        return (get(key) != null);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map
     * contains no mapping for the key.
     * @param key the name of the system attribute to retrieve
     * @throws IllegalArgumentException if given key is non-String
     */
    @Override
    @Nullable
    public String get(Object key) {
        if (!(key instanceof String)) {
            throw new IllegalArgumentException(
                    "Type of key [" + key.getClass().getName() + "] must be java.lang.String");
        }
        return getSystemAttribute((String) key);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Template method that returns the underlying system attribute.
     * <p>Implementations typically call {@link System#getProperty(String)} or {@link System#getenv(String)} here.
     */
    @Nullable
    protected abstract String getSystemAttribute(String attributeName);


    // Unsupported

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return Collections.emptySet();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> values() {
        return Collections.emptySet();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return Collections.emptySet();
    }

}

