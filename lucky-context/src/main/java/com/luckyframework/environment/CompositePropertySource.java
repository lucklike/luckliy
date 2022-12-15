package com.luckyframework.environment;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * 复合型属性原
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/13 03:37
 */
public class CompositePropertySource extends PropertySource<MutablePropertySources> {


    public CompositePropertySource(String name, MutablePropertySources source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        final AtomicReference<Object> value = new AtomicReference<>(null);
        for (PropertySource<?> ps : source) {
            if (ps.containsProperty(name)) {
                value.set(ps.getProperty(name));
                break;
            }
        }
        return value.get();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return source.size();
    }

    public Iterator<PropertySource<?>> iterator() {
        return source.iterator();
    }

    public Spliterator<PropertySource<?>> spliterator() {
        return source.spliterator();
    }

    public Stream<PropertySource<?>> stream() {
       return source.stream();
    }

    public boolean contains(String name) {
        return source.contains(name);
    }

    @Nullable
    public PropertySource<?> get(String name) {
        return source.get(name);
    }


    /**
     * Add the given property source object with the highest precedence.
     */
    public void addFirst(PropertySource<?> propertySource) {
        source.addFirst(propertySource);
    }

    /**
     * Add the given property source object with the lowest precedence.
     */
    public void addLast(PropertySource<?> propertySource) {
        source.addLast(propertySource);
    }

    /**
     * Add the given property source object with precedence immediately higher
     * than the named relative property source.
     */
    public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
        source.addBefore(relativePropertySourceName, propertySource);
    }

    /**
     * Add the given property source object with precedence immediately lower
     * than the named relative property source.
     */
    public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
        source.addAfter(relativePropertySourceName, propertySource);
    }

    /**
     * Return the precedence of the given property source, {@code -1} if not found.
     */
    public int precedenceOf(PropertySource<?> propertySource) {
        return source.precedenceOf(propertySource);
    }

    /**
     * Remove and return the property source with the given name, {@code null} if not found.
     * @param name the name of the property source to find and remove
     */
    @Nullable
    public PropertySource<?> remove(String name) {
        return source.remove(name);
    }

    /**
     * Replace the property source with the given name with the given property source object.
     * @param name the name of the property source to find and replace
     * @param propertySource the replacement property source
     * @throws IllegalArgumentException if no property source with the given name is present
     * @see #contains
     */
    public void replace(String name, PropertySource<?> propertySource) {
        source.replace(name, propertySource);
    }

    @Override
    public boolean containsProperty(String name) {
        for (PropertySource<?> propertySource : source) {
            if(propertySource.containsProperty(name)){
                return true;
            }
        }
        return false;
    }
}
