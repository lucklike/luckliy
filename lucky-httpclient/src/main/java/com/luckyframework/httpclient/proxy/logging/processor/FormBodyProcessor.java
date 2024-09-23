package com.luckyframework.httpclient.proxy.logging.processor;

public class FormBodyProcessor extends AbstractBodyProcessor {

    public FormBodyProcessor() {
    }

    public FormBodyProcessor(String translation) {
        super(translation);
    }

    @Override
    public String process(String body) {
        return body.replace("&", "&" + translation);
    }
}
