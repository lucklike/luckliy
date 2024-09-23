package com.luckyframework.httpclient.proxy.logging.processor;

public class FormBodyProcessor extends AbstractBodyProcessor {

    @Override
    public String process(String body) {
        return body.replace("&", "&" + translation);
    }
}
