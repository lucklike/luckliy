package com.luckyframework.validator.bean;

import com.luckyframework.context.message.MessageSource;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.util.Assert;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Implementation of Hibernate Validator 4.3/5.x's {@link ResourceBundleLocator} interface,
 * exposing a Spring {@link MessageSource} as localized {@link MessageSourceResourceBundle}.
 *
 * @author Juergen Hoeller
 * @since 3.0.4
 * @see ResourceBundleLocator
 * @see MessageSource
 * @see MessageSourceResourceBundle
 */
public class MessageSourceResourceBundleLocator implements ResourceBundleLocator {

    private final MessageSource messageSource;

    /**
     * Build a MessageSourceResourceBundleLocator for the given MessageSource.
     * @param messageSource the Spring MessageSource to wrap
     */
    public MessageSourceResourceBundleLocator(MessageSource messageSource) {
        Assert.notNull(messageSource, "MessageSource must not be null");
        this.messageSource = messageSource;
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return new MessageSourceResourceBundle(this.messageSource, locale);
    }

}
