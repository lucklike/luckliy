package com.luckyframework.webmvc.view;

import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/7 16:40
 */
public interface ViewResolver {

    @Nullable
    View resolveViewName(String viewName, Locale locale) throws Exception;
}
