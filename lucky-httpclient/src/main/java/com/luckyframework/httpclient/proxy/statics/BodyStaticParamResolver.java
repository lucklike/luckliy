package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.StaticBody;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import org.apache.http.entity.mime.content.StringBody;

import java.nio.charset.Charset;
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
        String charsetStr = context.parseExpression(staticBodyAnn.charset(), String.class);
        Object body = context.parseExpression(staticBodyAnn.body());
        Charset charset = StringUtils.hasText(charsetStr) ? Charset.forName(charsetStr) : null;

        BodyHandle bodyHandle = context.generateObject(staticBodyAnn.bodyHandle());
        byte[] afterProcessingBody = bodyHandle.handle(body, charset);
        return Collections.singletonList(new ParamInfo("body", BodyObject.builder(mimeType, charset, afterProcessingBody)));
    }

    /**
     * 请求体处理器
     */
    @FunctionalInterface
    interface BodyHandle {
        byte[] handle(Object body, Charset charset);
    }

    public abstract static class StringBodyHandle implements BodyHandle {

        @Override
        public byte[] handle(Object body, Charset charset) {
            return stringBody(body).getBytes(charset);
        }
        protected abstract String stringBody(Object body);
    }

    /**
     * 默认请请求体处理器，啥也不做直接转为String
     */
    public static class DefaultBodyHandle extends StringBodyHandle {
        @Override
        public String stringBody(Object body) {
            return String.valueOf(body);
        }
    }
}
