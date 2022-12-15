package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.AbstractPropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/14 09:12
 */
@SuppressWarnings("all")
public class ObjectPropertySourcesPropertyResolver extends PropertySourcesPropertyResolver{

    private final Function<Object,Object> function = text -> (text == null || text == ConfigurationMap. NULL_ENTRY) ? null : placeholderToObject(text);

    @Nullable
    private ObjectPropertyPlaceholderHelper nonObjectStrictHelper;

    @Nullable
    private ObjectPropertyPlaceholderHelper objectStrictHelper;

    @Nullable
    private final PropertySources propertySources;

    private String thisPlaceholderPrefix;
    private String thisPlaceholderSuffix;
    private String thisValueSeparator;


    /**
     * Create a new resolver against the given property sources.
     *
     * @param propertySources the set of {@link PropertySource} objects to use
     */
    public ObjectPropertySourcesPropertyResolver(PropertySources propertySources) {
        super(propertySources);
        this.propertySources = propertySources;
    }

    @Nullable
    public Object getPropertyForObject(Object property) {
        return getPropertyForType(property, Object.class);
    }

    @Nullable
    public PropertySources getPropertySources() {
        return propertySources;
    }

    @Nullable
    public <T> T getPropertyForType(Object property, ResolvableType targetValueType) {
        return getPropertyForType(property, targetValueType, true);
    }

    @Nullable
    public <T> T getPropertyForType(Object property, Class<T> targetValueType) {
        return getPropertyForType(property, ResolvableType.forRawClass(targetValueType), true);
    }

    @Nullable
    public <T> T getPropertyForType(Object property, SerializationTypeToken<T> targetValueTypeToken) {
        return getPropertyForType(property, ResolvableType.forType(targetValueTypeToken.getType()), true);
    }

    public Object getRequiredPropertyForObject(Object property) throws IllegalStateException{
        return getRequiredPropertyForType(property, Object.class);
    }

    public <T> T getRequiredPropertyForType(Object property, ResolvableType type) throws IllegalStateException {
        Object value = getPropertyForType(property, type);
        if (value == null) {
            throw new IllegalStateException("Required key '" + property + "' not found");
        }
        return (T) value;
    }

    public <T> T getRequiredPropertyForType(Object property, Class<T> type) throws IllegalStateException{
        return getRequiredPropertyForType(property, ResolvableType.forRawClass(type));
    }

    public <T> T getRequiredPropertyForType(Object property, SerializationTypeToken<T> typeToken) throws IllegalStateException{
        return getRequiredPropertyForType(property, ResolvableType.forType(typeToken.getType()));
    }


    @Nullable
    protected <T> T getPropertyForType(Object key, ResolvableType targetValueType, boolean resolveNestedPlaceholders) {

        if(!(key instanceof String)){
            return (T) ConversionUtils.conversion(key, targetValueType, function);
        }

        String keyString = (String) key;
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Searching for key '" + keyString + "' in PropertySource '" +
                            propertySource.getName() + "'");
                }
                Object value = propertySource.getProperty(keyString);
                if (value != null) {
                    if (resolveNestedPlaceholders && value instanceof String) {
                        value = placeholderToObject((String) value);
                    }
                    if(!(value instanceof EnvData)){
                        if(value instanceof Map){
                            value = new EnvSourceMap(this, keyString);
                        }else if((value instanceof Collection) || value.getClass().isArray()){
                            value = new EnvSourceList(this, keyString);
                        }
                    }
                    logKeyFound(keyString, propertySource, value);
                    return (T) ConversionUtils.conversion(value, targetValueType, function);
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Could not find key '" + key + "' in any property source");
        }
        return null;
    }

    public Function<Object, Object> getConversionFunction() {
        return function;
    }

    public Object placeholderToObject(Object placeholerObj) {
        if (this.nonObjectStrictHelper == null) {
            this.nonObjectStrictHelper = createPlaceholderHelper(true);
        }
        return doPlaceholderToObject(placeholerObj, this.nonObjectStrictHelper);
    }

    public Object requiredPlaceholderToObject(Object placeholerObj) throws IllegalArgumentException {
        if (this.objectStrictHelper == null) {
            this.objectStrictHelper = createPlaceholderHelper(false);
        }
        return doPlaceholderToObject(placeholerObj, this.objectStrictHelper);
    }

    private Object doPlaceholderToObject(Object placeholerObj, ObjectPropertyPlaceholderHelper helper) {
        return helper.replacePlaceholders(placeholerObj, new ObjectPropertyPlaceholderHelper.PlaceholderResolver() {
            @Override
            public Object resolvePlaceholder(String placeholderName) {
                return getPropertyForObject(placeholderName);
            }

            @Override
            public boolean propertyIsExist(String placeholderName) {
                return containsProperty(placeholderName);
            }
        });
    }

    private ObjectPropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
        return new ObjectPropertyPlaceholderHelper(getSupperPlaceholderPrefix(), getSupperPlaceholderSuffix(),
                getSupperValueSeparator(), ignoreUnresolvablePlaceholders);
    }

    public String getSupperPlaceholderPrefix(){
        if(thisPlaceholderPrefix == null){
            thisPlaceholderPrefix = getSupperStringField("placeholderPrefix");
        }
        return thisPlaceholderPrefix;
    }

    public String getSupperPlaceholderSuffix(){
        if(thisPlaceholderSuffix == null){
            thisPlaceholderSuffix = getSupperStringField("placeholderSuffix");
        }
        return thisPlaceholderSuffix;
    }

    public String getSupperValueSeparator(){
        if(thisValueSeparator == null){
            thisValueSeparator = getSupperStringField("valueSeparator");
        }
        return thisValueSeparator;
    }

    private String getSupperStringField(String fieldName){
        Field field = FieldUtils.getDeclaredField(AbstractPropertyResolver.class, fieldName);
        Object fieldValue = FieldUtils.getValue(this, field);
        return fieldValue == null ? null : fieldValue.toString();
    }


}
