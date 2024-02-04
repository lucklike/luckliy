package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 可以返回Object属性的属性占位符解析器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/14 10:18
 */
public class ObjectPropertyPlaceholderHelper {

    private static final Log logger = LogFactory.getLog(ObjectPropertyPlaceholderHelper.class);

    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);

    static {
        wellKnownSimplePrefixes.put("}", "{");
        wellKnownSimplePrefixes.put("]", "[");
        wellKnownSimplePrefixes.put(")", "(");
    }


    private final String placeholderPrefix;

    private final String placeholderSuffix;

    private final String simplePrefix;

    @Nullable
    private final String valueSeparator;

    private final boolean ignoreUnresolvablePlaceholders;


    /**
     * Creates a new {@code ObjectPropertyPlaceholderHelper} that uses the supplied prefix and suffix.
     * Unresolvable placeholders are ignored.
     * @param placeholderPrefix the prefix that denotes the start of a placeholder
     * @param placeholderSuffix the suffix that denotes the end of a placeholder
     */
    public ObjectPropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
        this(placeholderPrefix, placeholderSuffix, null, true);
    }

    /**
     * Creates a new {@code ObjectPropertyPlaceholderHelper} that uses the supplied prefix and suffix.
     * @param placeholderPrefix the prefix that denotes the start of a placeholder
     * @param placeholderSuffix the suffix that denotes the end of a placeholder
     * @param valueSeparator the separating character between the placeholder variable
     * and the associated default value, if any
     * @param ignoreUnresolvablePlaceholders indicates whether unresolvable placeholders should
     * be ignored ({@code true}) or cause an exception ({@code false})
     */
    public ObjectPropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix,
                                           @Nullable String valueSeparator, boolean ignoreUnresolvablePlaceholders) {

        Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
        Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
        if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
            this.simplePrefix = simplePrefixForSuffix;
        }
        else {
            this.simplePrefix = this.placeholderPrefix;
        }
        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    /**
     * Replaces all placeholders of format {@code ${name}} with the corresponding
     * property from the supplied {@link Properties}.
     * @param value the value containing the placeholders to be replaced
     * @param properties the {@code Properties} to use for replacement
     * @return the supplied value with placeholders replaced inline
     */
    public Object replacePlaceholders(String value, final Properties properties) {
        Assert.notNull(properties, "'properties' must not be null");
        return replacePlaceholders(value, new PlaceholderResolver() {
            @Override
            public Object resolvePlaceholder(String placeholderName) {
                return properties.getProperty(placeholderName);
            }

            @Override
            public boolean propertyIsExist(String placeholderName) {
                return properties.containsKey(placeholderName);
            }
        });
    }

    /**
     * Replaces all placeholders of format {@code ${name}} with the corresponding
     * property from the supplied {@link ConfigurationMap}.
     * @param value the value containing the placeholders to be replaced
     * @param configMap the {@code ConfigurationMap} to use for replacement
     * @return the supplied value with placeholders replaced inline
     */
    public Object replacePlaceholders(Object value, final ConfigurationMap configMap) {
        Assert.notNull(configMap, "'properties' must not be null");
        return replacePlaceholders(value, new PlaceholderResolver() {
            @Override
            public Object resolvePlaceholder(String placeholderName) {
                return configMap.getProperty(placeholderName);
            }

            @Override
            public boolean propertyIsExist(String placeholderName) {
                return configMap.containsConfigKey(placeholderName);
            }
        });
    }

    /**
     * Replaces all placeholders of format {@code ${name}} with the value returned
     * from the supplied {@link PlaceholderResolver}.
     * @param value the value containing the placeholders to be replaced
     * @param placeholderResolver the {@code PlaceholderResolver} to use for replacement
     * @return the supplied value with placeholders replaced inline
     */
    public Object replacePlaceholders(Object value, PlaceholderResolver placeholderResolver) {
        Assert.notNull(value, "'value' must not be null");
        return parseObjectValue(value, placeholderResolver, null);
    }

    protected Object parseObjectValue(
            Object value, PlaceholderResolver placeholderResolver, @Nullable Set<String> visitedPlaceholders) {
        if(value == null){
            return null;
        }
        if(value instanceof String){

            String strVal = (String) value;

            int startIndex = strVal.indexOf(this.placeholderPrefix);
            if (startIndex == -1) {
                return strVal;
            }

            StringBuilder result = new StringBuilder(strVal);
            while (startIndex != -1) {
                int endIndex = findPlaceholderEndIndex(result, startIndex);
                if (endIndex != -1) {
                    Object placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                    String originalPlaceholder = (String) placeholder;
                    if (visitedPlaceholders == null) {
                        visitedPlaceholders = new HashSet<>(4);
                    }
                    if (!visitedPlaceholders.add(originalPlaceholder)) {
                        throw new IllegalArgumentException(
                                "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
                    }
                    // Recursive invocation, parsing placeholders contained in the placeholder key.
                    placeholder = parseObjectValue(placeholder, placeholderResolver, visitedPlaceholders);
                    // Now obtain the value for the fully resolved key...
                    String strPlaceholder = placeholder.toString();
                    Object propVal = placeholderResolver.resolvePlaceholder(strPlaceholder);
                    boolean actualPropertyIsExist = placeholderResolver.propertyIsExist(strPlaceholder);
                    if (propVal == null && this.valueSeparator != null) {
                        int separatorIndex = originalPlaceholder.indexOf(this.valueSeparator);
                        if (separatorIndex != -1) {
                            actualPropertyIsExist = true;

                            String actualPlaceholder = originalPlaceholder.substring(0, separatorIndex);
                            String defaultPlaceholder = originalPlaceholder.substring(separatorIndex + this.valueSeparator.length());

                            if(placeholderResolver.propertyIsExist(actualPlaceholder)){
                                propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                            }else{
                                propVal = parseObjectValue(defaultPlaceholder, placeholderResolver, visitedPlaceholders);
                                propVal = specialValueConversion(propVal);
                            }
                        }
                    }
                    if (propVal != null) {
                        if(startIndex == 0 && endIndex == strVal.length() - 1){
                            return propVal;
                        }
                        // Recursive invocation, parsing placeholders contained in the
                        // previously resolved placeholder value.
                        propVal = parseObjectValue(propVal, placeholderResolver, visitedPlaceholders);
                        String strPropVal = propVal.toString();
                        result.replace(startIndex, endIndex + this.placeholderSuffix.length(), strPropVal);
                        if (logger.isTraceEnabled()) {
                            logger.trace("Resolved placeholder '" + placeholder + "'");
                        }
                        startIndex = result.indexOf(this.placeholderPrefix, startIndex + strPropVal.length());
                    }
                    // key在配置中存在，但对应的value为null
                    else if(actualPropertyIsExist){
                        if(startIndex == 0 && endIndex == strVal.length() - 1){
                            return null;
                        }
                        String strPropVal = "null";
                        result.replace(startIndex, endIndex + this.placeholderSuffix.length(), strPropVal);
                        if (logger.isTraceEnabled()) {
                            logger.trace("Resolved placeholder '" + placeholder + "'");
                        }
                        startIndex = result.indexOf(this.placeholderPrefix, startIndex + strPropVal.length());
                    }
                    else if (this.ignoreUnresolvablePlaceholders) {
                        // Proceed with unprocessed value.
                        startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                    }
                    else {
                        throw new IllegalArgumentException("Could not resolve placeholder '" +
                                placeholder + "'" + " in value \"" + value + "\"");
                    }
                    visitedPlaceholders.remove(originalPlaceholder);
                }
                else {
                    startIndex = -1;
                }
            }
            return result.toString();
        }

        Class<?> valueClass = value.getClass();
        if(valueClass.isArray()){
            int length = Array.getLength(value);
            Object propArray = Array.newInstance(valueClass.getComponentType(), length);
            for (int i = 0; i < length; i++) {
                Array.set(propArray, i, parseObjectValue(Array.get(value, i), placeholderResolver, visitedPlaceholders));
            }
            return propArray;
        }
        if(Collection.class.isAssignableFrom(valueClass)){
            Collection valueCollection = (Collection) value;
            List propList = new LinkedList();
            for (Object obj : valueCollection) {
                propList.add(parseObjectValue(obj, placeholderResolver, visitedPlaceholders));
            }
            if(Set.class.isAssignableFrom(valueClass)){
                return new LinkedHashSet<>(propList);
            }
            return propList;
        }
        if(Map.class.isAssignableFrom(valueClass)){
            Map<?, ?> valueMap = (Map<? ,?>) value;
            Map propMap = new ConfigurationMap();
            for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
                propMap.put(parseObjectValue(entry.getKey(), placeholderResolver, visitedPlaceholders), parseObjectValue(entry.getValue(), placeholderResolver, visitedPlaceholders));
            }
            return propMap;
        }

        return value;

    }


    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                }
                else {
                    return index;
                }
            }
            else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
                withinNestedPlaceholder++;
                index = index + this.simplePrefix.length();
            }
            else {
                index++;
            }
        }
        return -1;
    }


    private Object specialValueConversion(Object value){
        if("".equals(value)){
            return null;
        }
        if("''".equals(value) || "\"\"".equals(value)){
            return "";
        }
        return value;
    }


    /**
     * Strategy interface used to resolve replacement values for placeholders contained in Strings.
     */
    public interface PlaceholderResolver {

        /**
         * Resolve the supplied placeholder name to the replacement value.
         * @param placeholderName the name of the placeholder to resolve
         * @return the replacement value, or {@code null} if no replacement is to be made
         */
        @Nullable
        Object resolvePlaceholder(String placeholderName);

        boolean propertyIsExist(String placeholderName);
    }



}
