package com.luckyframework.httpclient.proxy.logging.processor;

import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.logging.BodyProcessor;

public abstract class AbstractBodyProcessor implements BodyProcessor<String> {

    protected String translation;
    protected String colorCore;

    public String getColorCore() {
        return colorCore;
    }

    public void setColorCore(String colorCore) {
        this.colorCore = colorCore;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String convert(Request request) {
        BodyObject body = request.getBody();
        return body == null ? "" : body.getBodyAsString();
    }
}
