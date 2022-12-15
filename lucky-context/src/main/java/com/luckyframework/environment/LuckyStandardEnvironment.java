package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.conversion.ConversionUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.lang.Nullable;

import java.util.function.Function;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/11 17:15
 */
@SuppressWarnings("all")
public class LuckyStandardEnvironment extends AbstractConfigurableEnvironment implements ObjectPropertyResolver {

    /** System environment property source name: {@value}. */
    public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

    /** JVM system properties property source name: {@value}. */
    public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";

    private final ObjectPropertySourcesPropertyResolver objectPropertyResolver;


    /**
     * Create a new {@code StandardEnvironment} instance with a default
     * {@link MutablePropertySources} instance.
     */
    public LuckyStandardEnvironment() {
       this(new MutablePropertySources());
    }

    /**
     * Create a new {@code StandardEnvironment} instance with a specific
     * {@link MutablePropertySources} instance.
     * @param propertySources property sources to use
     * @since 5.3.4
     */
    protected LuckyStandardEnvironment(MutablePropertySources propertySources) {
        super(propertySources);
        objectPropertyResolver = createObjectPropertyResolver(propertySources);
    }

    public Function<Object, Object> getFunction() {
        return this.objectPropertyResolver.getConversionFunction();
    }

    /**
     * Customize the set of property sources with those appropriate for any standard
     * Java environment:
     * <ul>
     * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
     * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
     * </ul>
     * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
     * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
     * @see AbstractConfigurableEnvironment#customizePropertySources(MutablePropertySources)
     * @see #getSystemProperties()
     * @see #getSystemEnvironment()
     */
    @Override
    protected void customizePropertySources(MutablePropertySources propertySources) {
        propertySources.addLast(
                new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, new ConfigurationMap(getSystemProperties()).toProperties()));
        propertySources.addLast(
                new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
    }

    //----------------------------------------------------------------
    //              get property for type
    //----------------------------------------------------------------

    @Nullable
    public <T> T getPropertyForType(Object property, ResolvableType type){
        return this.objectPropertyResolver.getPropertyForType(property, type);
    }

    //----------------------------------------------------------------
    //              get require property for type
    //----------------------------------------------------------------

    public <T> T getRequiredPropertyForType(Object property, ResolvableType type){
        return this.objectPropertyResolver.getRequiredPropertyForType(property, type);
    }


    //----------------------------------------------------------------
    //              resolve placeholders for type
    //----------------------------------------------------------------

    public Object resolvePlaceholdersForObject(Object placeholderObj) {
        return this.objectPropertyResolver.placeholderToObject(placeholderObj);
    }

    public <T> T resolvePlaceholdersForType(Object placeholderObj, ResolvableType type) {
        return (T) ConversionUtils.conversion(resolvePlaceholdersForObject(placeholderObj), type, getFunction());
    }


    //----------------------------------------------------------------
    //              resolve required placeholders for type
    //----------------------------------------------------------------

    public Object resolveRequiredPlaceholdersForObject(Object placeholderObj) throws IllegalArgumentException {
        return this.objectPropertyResolver.requiredPlaceholderToObject(placeholderObj);
    }

    public <T> T resolveRequiredPlaceholdersForType(Object placeholderObj, ResolvableType type) throws IllegalArgumentException {
        return (T) ConversionUtils.conversion(resolveRequiredPlaceholdersForObject(placeholderObj), type, getFunction());
    }

    public ObjectPropertySourcesPropertyResolver getObjectPropertyResolver() {
        return objectPropertyResolver;
    }

    /**
     * Factory method used to create the {@link ConfigurablePropertyResolver}
     * instance used by the Environment.
     * @since 5.3.4
     * @see #getPropertyResolver()
     */
    protected ObjectPropertySourcesPropertyResolver createObjectPropertyResolver(MutablePropertySources propertySources) {
        return new ObjectPropertySourcesPropertyResolver(propertySources);
    }

}
