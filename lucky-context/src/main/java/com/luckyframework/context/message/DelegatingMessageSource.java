package com.luckyframework.context.message;

import com.luckyframework.context.AbstractApplicationContext;
import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * Empty {@link MessageSource} that delegates all calls to the parent MessageSource.
 * If no parent is available, it simply won't resolve any message.
 *
 * <p>Used as placeholder by AbstractApplicationContext, if the context doesn't
 * define its own MessageSource. Not intended for direct use in applications.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see AbstractApplicationContext
 */
public class DelegatingMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

    @Nullable
    private MessageSource parentMessageSource;


    @Override
    public void setParentMessageSource(@Nullable MessageSource parent) {
        this.parentMessageSource = parent;
    }

    @Override
    @Nullable
    public MessageSource getParentMessageSource() {
        return this.parentMessageSource;
    }


    @Override
    @Nullable
    public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
        if (this.parentMessageSource != null) {
            return this.parentMessageSource.getMessage(code, args, defaultMessage, locale);
        }
        else if (defaultMessage != null) {
            return renderDefaultMessage(defaultMessage, args, locale);
        }
        else {
            return null;
        }
    }

    @Override
    public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
        if (this.parentMessageSource != null) {
            return this.parentMessageSource.getMessage(code, args, locale);
        }
        else {
            throw new NoSuchMessageException(code, locale);
        }
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        if (this.parentMessageSource != null) {
            return this.parentMessageSource.getMessage(resolvable, locale);
        }
        else {
            if (resolvable.getDefaultMessage() != null) {
                return renderDefaultMessage(resolvable.getDefaultMessage(), resolvable.getArguments(), locale);
            }
            String[] codes = resolvable.getCodes();
            String code = (codes != null && codes.length > 0 ? codes[0] : "");
            throw new NoSuchMessageException(code, locale);
        }
    }


    @Override
    public String toString() {
        return this.parentMessageSource != null ? this.parentMessageSource.toString() : "Empty MessageSource";
    }

}