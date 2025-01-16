package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.StaticBody;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

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
        Supplier<String> bodySupplier = bodyHandle.bodySupplier(body, afterProcessingBody, mimeType, charset);
        BodyObject bodyObject = BodyObject.builder(mimeType, charset, afterProcessingBody, bodySupplier);
        return Collections.singletonList(new ParamInfo("body", bodyObject));
    }

    /**
     * 请求体处理器
     */
    @FunctionalInterface
    public interface BodyHandle {
        @NonNull
        byte[] handle(Object body, Charset charset);

        default Supplier<String> bodySupplier(Object object, byte[] objBytes, String mimeType, @Nullable Charset charset) {
            return null;
        }

    }

    public abstract static class StringBodyHandle implements BodyHandle {

        @NonNull
        @Override
        public byte[] handle(Object body, Charset charset) {
            return stringBody(body).getBytes(charset);
        }

        @Override
        public Supplier<String> bodySupplier(Object object, byte[] objBytes, String mimeType, Charset charset) {
            return () -> new String(objBytes, charset == null ? StandardCharsets.UTF_8 : charset);
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
