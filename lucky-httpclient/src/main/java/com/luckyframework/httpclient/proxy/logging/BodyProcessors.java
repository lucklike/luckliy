package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.KeyCaseSensitivityMap;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.Request;

import java.util.HashMap;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/9/22 13:42
 */
@SuppressWarnings("all")
public class BodyProcessors {

    private final KeyCaseSensitivityMap<BodyProcessor<?>> processors = new KeyCaseSensitivityMap<>(new HashMap<>());


    public void addProcessor(String mimeType, BodyProcessor processor) {
        processors.put(mimeType, processor);
    }

    public void removeProcessor(String mimeType) {
        processors.remove(mimeType);
    }

    public BodyProcessor getProcessor(String mimeType) {
        return processors.get(mimeType);
    }

    public String process(Request request) {
        String mimeType = request.getContentType().getMimeType();
        BodyProcessor processor = getProcessor(mimeType);
        if (processor != null) {
            Object convertObject = processor.convert(request);
            return processor.process(convertObject);
        }
        BodyObject body = request.getBody();
        return body != null ? body.getBodyAsString() : "";
    }

}
