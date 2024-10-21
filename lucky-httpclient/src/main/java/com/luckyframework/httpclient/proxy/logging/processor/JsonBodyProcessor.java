package com.luckyframework.httpclient.proxy.logging.processor;

import com.luckyframework.serializable.JacksonSerializationScheme;

public class JsonBodyProcessor extends AbstractBodyProcessor {

    @Override
    public String process(String body) {
        try {
            String json = JacksonSerializationScheme.prettyPrinting(body);
            String first = translation + json.charAt(0);
            String last = translation + json.substring(json.length() - 1);
            String middle = json.substring(1, json.length() - 1).replace("\n ", "\n" + translation);
            return first + middle + last;
        } catch (Exception e) {
            return body;
        }
    }

}
