package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.InputStreamParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * InputStream参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:26
 */
public class InputStreamDynamicParamResolver extends AbstractDynamicParamResolver {


    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        Object originalParamValue = valueContext.getValue();
        InputStream[] inputStreams = ConversionUtils.conversion(originalParamValue, InputStream[].class);
        String filenameEx = context.toAnnotation(InputStreamParam.class).filename();
        Object filenameResult = context.parseExpression(filenameEx, arg -> arg.extractKeyValue("p", originalParamValue));

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
                i++;
            }
            return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), httpFiles));
        } else if (inputStreams.length == 1) {
            return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), new HttpFile[]{new HttpFile(inputStreams[0], String.valueOf(filenameResult))}));
        }
        throw new IllegalStateException("The filename expression is a file, but the input stream in the argument is an array.");
    }
}
