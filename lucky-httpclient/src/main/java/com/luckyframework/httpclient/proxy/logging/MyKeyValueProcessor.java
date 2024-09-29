package com.luckyframework.httpclient.proxy.logging;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/9/22 14:37
 */
public class MyKeyValueProcessor implements KeyValueProcessor {

    @Override
    public KV process(Object key, Object value) {
        List<Object> valueList = (List<Object>) value;
        List<Object> newValueList = new ArrayList<>();

        for (Object o : valueList) {
            newValueList.add(getV(o.toString().length()));
        }
        return new KV(key, newValueList);
    }

    private String getV(int l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l; i++) {
            sb.append("*");
        }
        return sb.toString();
    }

}
