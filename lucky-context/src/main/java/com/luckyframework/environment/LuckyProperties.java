package com.luckyframework.environment;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Static holder for local Spring properties, i.e. defined at the Spring library level.
 *
 * <p>Reads a {@code spring.properties} file from the root of the Spring library classpath,
 * and also allows for programmatically setting properties through {@link #setProperty}.
 * When checking a property, local entries are being checked first, then falling back
 * to JVM-level system properties through a {@link System#getProperty} check.
 *
 * <p>This is an alternative way to set Spring-related system properties such as
 * "spring.getenv.ignore" and "spring.beaninfo.ignore", in particular for scenarios
 * where JVM system properties are locked on the target platform (e.g. WebSphere).
 * See {@link #setFlag} for a convenient way to locally set such flags to "true".
 *
 * @author Juergen Hoeller
 * @since 3.2.7
 * @see org.springframework.beans.CachedIntrospectionResults#IGNORE_BEANINFO_PROPERTY_NAME
 * @see org.springframework.context.index.CandidateComponentsIndexLoader#IGNORE_INDEX
 * @see org.springframework.core.env.AbstractEnvironment#IGNORE_GETENV_PROPERTY_NAME
 * @see org.springframework.expression.spel.SpelParserConfiguration#SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
 * @see org.springframework.jdbc.core.StatementCreatorUtils#IGNORE_GETPARAMETERTYPE_PROPERTY_NAME
 * @see org.springframework.jndi.JndiLocatorDelegate#IGNORE_JNDI_PROPERTY_NAME
 * @see org.springframework.objenesis.SpringObjenesis#IGNORE_OBJENESIS_PROPERTY_NAME
 * @see org.springframework.test.context.NestedTestConfiguration#ENCLOSING_CONFIGURATION_PROPERTY_NAME
 * @see org.springframework.test.context.TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
 * @see org.springframework.test.context.cache.ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
 */
public final class LuckyProperties {

    private static final String PROPERTIES_RESOURCE_LOCATION = "lucky.properties";

    private static final Properties localProperties = new Properties();


    static {
        try {
            ClassLoader cl = org.springframework.core.SpringProperties.class.getClassLoader();
            URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
                    ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
            if (url != null) {
                try (InputStream is = url.openStream()) {
                    localProperties.load(is);
                }
            }
        }
        catch (IOException ex) {
            System.err.println("Could not load 'spring.properties' file from local classpath: " + ex);
        }
    }


    private LuckyProperties() {
    }


    /**
     * Programmatically set a local property, overriding an entry in the
     * {@code spring.properties} file (if any).
     * @param key the property key
     * @param value the associated property value, or {@code null} to reset it
     */
    public static void setProperty(String key, @Nullable String value) {
        if (value != null) {
            localProperties.setProperty(key, value);
        }
        else {
            localProperties.remove(key);
        }
    }

    /**
     * Retrieve the property value for the given key, checking local Spring
     * properties first and falling back to JVM-level system properties.
     * @param key the property key
     * @return the associated property value, or {@code null} if none found
     */
    @Nullable
    public static String getProperty(String key) {
        String value = localProperties.getProperty(key);
        if (value == null) {
            try {
                value = System.getProperty(key);
            }
            catch (Throwable ex) {
                System.err.println("Could not retrieve system property '" + key + "': " + ex);
            }
        }
        return value;
    }

    /**
     * Programmatically set a local flag to "true", overriding an
     * entry in the {@code spring.properties} file (if any).
     * @param key the property key
     */
    public static void setFlag(String key) {
        localProperties.put(key, Boolean.TRUE.toString());
    }

    /**
     * Retrieve the flag for the given property key.
     * @param key the property key
     * @return {@code true} if the property is set to "true",
     * {@code} false otherwise
     */
    public static boolean getFlag(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

}
