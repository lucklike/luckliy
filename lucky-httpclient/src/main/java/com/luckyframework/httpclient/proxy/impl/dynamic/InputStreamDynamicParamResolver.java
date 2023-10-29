package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.httpclient.proxy.ValueContext;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * InputStream参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:26
 */
public class InputStreamDynamicParamResolver extends AbstractDynamicParamResolver {

    private static final String FILE_NAME = "filename";

    @Override
    public List<ParamInfo> doParser(ValueContext context, Annotation dynamicParamAnn) {
        Object originalParamValue = context.getValue();
        InputStream[] inputStreams = ConversionUtils.conversion(originalParamValue, InputStream[].class);

        String filenameEx = context.getAnnotationAttribute(dynamicParamAnn, FILE_NAME, String.class);
        SpELConvert spELConverter = HttpClientProxyObjectFactory.getSpELConverter();

        Map<String, Object> parameters = new HashMap<>(1);
        parameters.put("p", originalParamValue);
        Object filenameResult = spELConverter.parseExpression(filenameEx, parameters);

        if (ContainerUtils.isIterable(filenameResult)) {
            int filenameLength = ContainerUtils.getIteratorLength(filenameResult);
            if (inputStreams.length != filenameLength) {
                throw new IllegalStateException("The length of the name returned by the filename expression does not match the length of the input stream array.");
            }
            HttpFile[] httpFiles = new HttpFile[filenameLength];
            Iterator<Object> iterator = ContainerUtils.getIterator(filenameResult);
            int i = 0;
            while (iterator.hasNext()) {
                httpFiles[i] = new HttpFile(inputStreams[i], String.valueOf(iterator.next()));
            }
            return Collections.singletonList(new ParamInfo(getOriginalParamName(context), httpFiles));
        } else if (inputStreams.length == 1) {
            return Collections.singletonList(new ParamInfo(getOriginalParamName(context), new HttpFile[] {new HttpFile(inputStreams[0], String.valueOf(filenameResult))}));
        }
        throw new IllegalStateException("The filename expression is a file, but the input stream in the argument is an array.");
    }
}
