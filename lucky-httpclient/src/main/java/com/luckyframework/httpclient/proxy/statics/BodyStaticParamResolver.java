package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.StaticBody;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import kotlin.random.Random;

import java.util.Collections;
import java.util.List;

/**
 * 静态Body参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/19 10:50
 */
public class BodyStaticParamResolver implements StaticParamResolver {
    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        StaticBody staticBodyAnn = context.getMergedAnnotationCheckParent(StaticBody.class);
        String mimeType = context.parseExpression(staticBodyAnn.mimeType(), String.class);
        String charset = context.parseExpression(staticBodyAnn.charset(), String.class);
        Object body = context.parseExpression(staticBodyAnn.body());
        BodyHandle bodyHandle = (BodyHandle) context.generateObject(staticBodyAnn.bodyHandle());
        String jsonBody = bodyHandle.handle(body);
        return Collections.singletonList(new ParamInfo("body", BodyObject.builder(mimeType, charset, jsonBody)));
    }

    /**
     * 请求体处理器
     */
    @FunctionalInterface
    interface BodyHandle {
        String handle(Object body);
    }

    /**
     * 默认请请求体处理器，啥也不做直接转为String
     */
    public static class DefaultBodyHandle implements BodyHandle {
        @Override
        public String handle(Object body) {
            return String.valueOf(body);
        }
    }
}
