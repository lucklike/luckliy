package com.luckyframework.webmvc.view;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/7 16:39
 */
public interface View {

    @Nullable
    default String getContentType() {
        return null;
    }

    void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
