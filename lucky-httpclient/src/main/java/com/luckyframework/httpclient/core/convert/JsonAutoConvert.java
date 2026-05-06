package com.luckyframework.httpclient.core.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;

import java.lang.reflect.Type;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * JSON响应数据自动转换器
 */
public class JsonAutoConvert implements Response.AutoConvert {
    @Override
    public boolean can(Response resp, Type type) {
        try {
            // 返回值为类型为【不能自动关闭资源的类型】时不做处理
            if (HttpClientProxyObjectFactory.getNotAutoCloseResourceTypes().contains(type)) {
                return false;
            }
            if (resp.isJsonBody()) {
                return true;
            }
            return isValidJson(resp.getStringResult());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public <T> T convert(Response resp, Type type) {
        return resp.jsonStrToEntity(type);
    }


    /**
     * 判断字符串是否为合法的 JSON 对象或数组
     *
     * @param str 待判断的字符串
     * @return true 表示是合法 JSON
     */
    public static boolean isValidJson(String str) {
        if (!StringUtils.hasText(str)) {
            return false;
        }

        String trimmed = str.trim();

        boolean b1 = trimmed.startsWith("{");
        boolean b2 = trimmed.endsWith("}");
        boolean b3 = trimmed.startsWith("[");
        boolean b4 = trimmed.endsWith("]");

        // 非 { [ 开头
        if (!b1 && !b3) {
            return false;
        }

        // 以 { 开头 但是非 } 结尾
        if (b1 && !b2) {
            return false;
        }

        // 以 [ 开头 但是非 ] 结尾
        if (b3 && !b4) {
            return false;
        }

        // 快速检查通过之后尝试进行序列化
        try {
            JSON_SCHEME.deserialization(str, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
