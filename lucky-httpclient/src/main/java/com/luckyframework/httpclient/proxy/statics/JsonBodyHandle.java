package com.luckyframework.httpclient.proxy.statics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * 静态JSON Body参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/19 10:50
 */
public class JsonBodyHandle extends BodyStaticParamResolver.StringBodyHandle {

    private final Gson gson = new GsonBuilder().create();

    @Override
    protected String stringBody(Object body) {
        if (body instanceof String){
            JsonElement je = JsonParser.parseString((String) body);
            return gson.toJson(je);
        }
        return gson.toJson(body);
    }
}
