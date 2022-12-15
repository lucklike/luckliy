package com.luckyframework.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;


@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {

    /**
     * The prefix of the properties that are valid to bind to this object. Synonym for
     * {@link #prefix()}. A valid prefix is defined by one or more words separated with
     * dots (e.g. {@code "acme.system.feature"}).
     * @return the prefix of the properties to bind
     */
    @AliasFor("prefix")
    String value() default "";

    /**
     * The prefix of the properties that are valid to bind to this object. Synonym for
     * {@link #value()}. A valid prefix is defined by one or more words separated with
     * dots (e.g. {@code "acme.system.feature"}).
     * @return the prefix of the properties to bind
     */
    @AliasFor("value")
    String prefix() default "";

    /**
     * Flag to indicate that when binding to this object invalid fields should be ignored.
     * Invalid means invalid according to the binder that is used, and usually this means
     * fields of the wrong type (or that cannot be coerced into the correct type).
     * @return the flag value (default false)
     */
    boolean ignoreInvalidFields() default false;

    /**
     * Flag to indicate that when binding to this object unknown fields should be ignored.
     * An unknown field could be a sign of a mistake in the Properties.
     * @return the flag value (default true)
     */
    boolean ignoreUnknownFields() default true;

    /**
     * Converts words separated by fixed separators to camel names
     */
    char[] toHump() default {'-','_'};

    /**
     *
     * The principle of priority matching, by default, the method is matched first, and then the attribute is matched
     */
    Type givePriorityMatch() default Type.METHOD;

    enum Type{
        METHOD,
        FIELD
    }
}
