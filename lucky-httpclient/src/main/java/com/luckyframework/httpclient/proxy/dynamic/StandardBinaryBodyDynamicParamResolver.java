package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.BinaryBody;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

/**
 * 标准二进制参数解析器
 * <pre>
 *   支持的参数类型如下：
 *      {@link byte[]}
 *      {@link Byte[]}
 *      {@link ByteBuffer}
 *      {@link Reader}
 *      {@link InputStream}
 *      {@link File}
 *      {@link InputStreamSource}
 *    如果参数不是以上类型，则会尝试使用{@code  ConversionUtils.conversion(object, Resource.class)}
 *    方法将参数转化为{@link Resource}类型
 *
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/30 02:45
 */
public class StandardBinaryBodyDynamicParamResolver extends AbstractDynamicParamResolver {

    /**
     * 默认的MimeType
     */
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {

        // 解析注解配置
        BinaryBody bodyParamAnn = context.toAnnotation(BinaryBody.class);
        String mimeType = context.parseExpression(bodyParamAnn.mimeType(), String.class);
        String charsetStr = context.parseExpression(bodyParamAnn.charset(), String.class);
        Charset charset = StringUtils.hasText(charsetStr) ? Charset.forName(charsetStr) : null;


        ValueContext valueContext = context.getContext();
        String paramName = getOriginalParamName(valueContext);
        Object value = valueContext.getValue();
        BodyObject bodyObject;
        if (value instanceof byte[]) {
            bodyObject = BodyObject.builder(mimeType, charset, (byte[]) value);
        } else if (value instanceof Byte[]) {
            Byte[] array = (Byte[]) value;
            byte[] bytes = new byte[array.length];
            for (int i = 0; i < array.length; i++) {
                bytes[i] = array[i];
            }
            bodyObject = BodyObject.builder(mimeType, charset, bytes);
        } else if (value instanceof ByteBuffer) {
            bodyObject = BodyObject.builder(mimeType, charset, ((ByteBuffer) value).array());
        } else if (value instanceof InputStream) {
            bodyObject = BodyObject.builder(mimeType, charset, (InputStream) value);
        } else if (value instanceof File) {
            try {
                File file = (File) value;

                String finalMimeTpe;
                if (!DEFAULT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
                    finalMimeTpe = mimeType;
                } else {
                    String fileMimeType = ContentTypeUtils.getMimeType(file.getName());
                    finalMimeTpe = fileMimeType == null ? DEFAULT_MIME_TYPE : fileMimeType;
                }
                bodyObject = BodyObject.builder(finalMimeTpe, charset, Files.newInputStream(file.toPath()));
                bodyObject.setStringSupplier(() -> StringUtils.format("File Body ({}) {}", UnitUtils.byteTo(file.length()), file.getAbsolutePath()));
            }catch (IOException e) {
                throw new LuckyRuntimeException(e, "Failed to parse the dynamic parameter '{}' : Failed to obtain the input stream from the file.", paramName);
            }

        } else if (value instanceof InputStreamSource) {
            try {
                bodyObject = BodyObject.builder(mimeType, charset, ((InputStreamSource) value));
            } catch (Exception e) {
                throw new LuckyRuntimeException(e, "Failed to parse the dynamic parameter '{}' : failed to obtain the input stream.", paramName);
            }
        } else if (value instanceof Reader) {
            try {
                bodyObject = BodyObject.builder(mimeType, charset, FileCopyUtils.copyToString((Reader) value));
            } catch (Exception e) {
                throw new LuckyRuntimeException(e, "Failed to parse dynamic parameter '{}' : failed to parse Reader.", paramName);
            }
        } else {
            try {
                Resource resource = ConversionUtils.conversion(String.valueOf(value), Resource.class);
                bodyObject = BodyObject.builder(mimeType, charset, resource);
            } catch (Exception e) {
                throw new LuckyRuntimeException(e, "Failed to parse dynamic parameter '{}' ", paramName);
            }
        }

        return Collections.singletonList(new ParamInfo(paramName, bodyObject));
    }


}
