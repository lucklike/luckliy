package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.httpclient.proxy.annotations.InputStreamParam;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * InputStream参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 17:47
 */
public class InputStreamParameterProcessor implements ParameterProcessor {

    @Override
    public HttpFile[] paramProcess(Object originalParam, Annotation proxyHttpParamAnn) {
        if (originalParam == null){
            return null;
        }
        InputStream[] inputStreams = ConversionUtils.conversion(originalParam, InputStream[].class);

        MergedAnnotation<?> mergedAnnotation = AnnotationUtils.getSpringRootMergedAnnotation(proxyHttpParamAnn);
        String filenameEx = mergedAnnotation.getString("filename");
        SpELConvert spELConverter = HttpClientProxyObjectFactory.getSpELConverter();

        Map<String, Object> parameters = new HashMap<>(1);
        parameters.put("p", originalParam);
        Object filenameResult = spELConverter.analyze(filenameEx, parameters);

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
            return httpFiles;
        } else if (inputStreams.length == 1) {
            return new HttpFile[] {new HttpFile(inputStreams[0], String.valueOf(filenameResult))};
        }
        throw new IllegalStateException("The filename expression is a file, but the input stream in the argument is an array.");
    }

    @Override
    public boolean needExpansionAnalysis() {
        return false;
    }
}
