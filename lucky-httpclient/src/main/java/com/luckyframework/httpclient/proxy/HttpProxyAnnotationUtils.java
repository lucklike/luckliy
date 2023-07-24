package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.core.BytesResultConvert;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestParameter;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.StringResultConvert;
import com.luckyframework.httpclient.core.impl.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.impl.DefaultRequestParameter;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.*;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 04:39
 */
public class HttpProxyAnnotationUtils {

    public static HttpRequestConfigContent getHttpRequestConfigContent(Class<?> aClass, Method method) {
        HttpConfiguration classConfigAnn = AnnotationUtils.findMergedAnnotation(aClass, HttpConfiguration.class);
        HttpConfiguration methodConfigAnn = AnnotationUtils.findMergedAnnotation(method, HttpConfiguration.class);
        return new HttpRequestConfigContent(classConfigAnn, methodConfigAnn);
    }

    public static boolean isIgnoreClassConvert(Class<?> aClass, Method method) {
        return getAnnotation(aClass, method, HttpRequest.class).ignoreClassConvert();
    }

    public static Request getRequest(Class<?> aClass, Method method, Object[] args) throws Exception {
        HttpRequest httpRequestAnn = getAnnotation(aClass, method, HttpRequest.class);
        RequestParameter requestParameter = new DefaultRequestParameter();
        HttpHeaderManager headerManager = new DefaultHttpHeaderManager();

        List<String> asmParamNameList = ASMUtil.getClassOrInterfaceMethodParamNames(method);
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = parameters[i];
            String asmParamName = asmParamNameList.get(i);

            Object parameterValue = args[i];
            // ResponseProcessor类型的参数略过
            if (parameterValue instanceof ResponseProcessor) {
                continue;
            }

            // Body参数设置
            BodyParam bodyParamAnn = AnnotationUtils.findMergedAnnotation(parameter, BodyParam.class);
            if (bodyParamAnn != null) {
                BodySerialization serialization = ClassUtils.newObject(bodyParamAnn.serializationScheme());
                requestParameter.setBody(BodyObject.builder(bodyParamAnn.mimeType(), bodyParamAnn.charset(), serialization.serialization(parameterValue)));
                continue;
            }
            if (parameterValue instanceof BodyObject){
                requestParameter.setBody((BodyObject) parameterValue);
                continue;
            }

            // 文件、资源类型参数设置
            String parameterName = getParameterName(parameter, asmParamName);
            Class<?> parameterType = parameter.getType();
            if(fileRequestParameterSetting(requestParameter, parameterName, parameterValue)){
                continue;
            }

            // http参数设置
            HttpParam httpParamAnn = AnnotationUtils.findMergedAnnotation(parameter, HttpParam.class);
            ParamLocation location = httpParamAnn == null ? ParamLocation.QUERY : httpParamAnn.location();
            requestParameterSetting(requestParameter, headerManager, location, parameterName, parameterValue);
        }
        return getRequest(httpRequestAnn, getHttpRequestConfigContent(aClass, method), headerManager, requestParameter);
    }

    private static void requestParameterSetting(RequestParameter requestParameter,
                                                HttpHeaderManager headerManager,
                                                ParamLocation location,
                                                String parameterName,
                                                Object parameterValue) {
        if (parameterValue == null) {
            return;
        }
        if(fileRequestParameterSetting(requestParameter, parameterName, parameterValue)) {
            return;
        }
        if (ClassUtils.isSimpleBaseType(parameterValue.getClass())) {
            baseTypeRequestParameterSetting(requestParameter, headerManager, location, parameterName, parameterValue);
        }
        else if (parameterValue instanceof Map) {
            mapRequestParameterSetting(requestParameter, headerManager, location, (Map<?, ?>) parameterValue);
        }
        else if (ContainerUtils.isIterable(parameterValue)) {
            iterableRequestParameterSetting(requestParameter, headerManager, location, parameterName, parameterValue);
        }
        else {
            entityRequestParameterSetting(requestParameter, headerManager, location, parameterValue);
        }
    }

    private static void mapRequestParameterSetting(RequestParameter requestParameter,
                                                   HttpHeaderManager headerManager,
                                                   ParamLocation location,
                                                   Map<?, ?> mapValue) {
        if(mapValue == null) {
            return;
        }
        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            requestParameterSetting(requestParameter, headerManager, location, key, value);
        }
    }

    private static void entityRequestParameterSetting(RequestParameter requestParameter,
                                                      HttpHeaderManager headerManager,
                                                      ParamLocation location,
                                                      Object entity) {
        if (entity == null) {
            return;
        }
        for (Field field : ClassUtils.getAllFields(entity.getClass())) {
            TempPair<String, ParamLocation> pair = getParamNameAndLocation(field, location);
            Object fieldValue = FieldUtils.getValue(entity, field);
            requestParameterSetting(requestParameter, headerManager, pair.getTwo(), pair.getOne(), fieldValue);
        }
    }

    private static void baseTypeRequestParameterSetting(RequestParameter requestParameter,
                                                        HttpHeaderManager headerManager,
                                                        ParamLocation location,
                                                        String parameterName,
                                                        Object parameterValue) {
        switch (location) {
            case PATH:
                requestParameter.addPathParameter(parameterName, parameterValue);
                break;
            case HEADER:
                headerManager.addHeader(parameterName, parameterValue);
                break;
            case FILE:
                requestParameter.addResources(parameterName, ConversionUtils.conversion(parameterValue, Resource[].class));
                break;
            default:
                requestParameter.addQueryParameter(parameterName, parameterValue);
        }
    }

    private static void iterableRequestParameterSetting(RequestParameter requestParameter,
                                                        HttpHeaderManager headerManager,
                                                        ParamLocation location,
                                                        String parameterName,
                                                        Object iterable) {
        Iterator<Object> iterator = ContainerUtils.getIterator(iterable);
        while (iterator.hasNext()) {
            Object next = iterator.next();
            requestParameterSetting(requestParameter, headerManager, location, parameterName, next);
        }

    }

    private static boolean fileRequestParameterSetting(RequestParameter requestParameter,
                                                       String parameterName,
                                                       Object parameterValue) {
        if (parameterValue instanceof File) {
            requestParameter.addFiles(parameterName, (File) parameterValue);
            return true;
        }
        if (parameterValue instanceof File[]) {
            requestParameter.addFiles(parameterName, (File[]) parameterValue);

        }
        if (parameterValue instanceof MultipartFile) {
            requestParameter.addMultipartFiles(parameterName, (MultipartFile) parameterValue);
            return true;
        }
        if (parameterValue instanceof MultipartFile[]) {
            requestParameter.addMultipartFiles(parameterName, (MultipartFile[]) parameterValue);
            return true;
        }
        if (parameterValue instanceof Resource) {
            requestParameter.addResources(parameterName, (Resource) parameterValue);
            return true;
        }
        if (parameterValue instanceof Resource[]) {
            requestParameter.addResources(parameterName, (Resource[]) parameterValue);
            return true;
        }
        if (ContainerUtils.isCollection(parameterValue)) {
            Class<?> paramGenericType = ResolvableType.forClass(Collection.class, parameterValue.getClass()).getGeneric(0).getRawClass();
            assert paramGenericType != null;
            if (File.class == paramGenericType) {
                requestParameter.addRequestParameter(parameterName, ConversionUtils.conversion(parameterValue, File[].class));
                return true;
            }
            if (MultipartFile.class == paramGenericType) {
                requestParameter.addRequestParameter(parameterName, ConversionUtils.conversion(parameterValue, MultipartFile[].class));
                return true;
            }
            if (Resource.class.isAssignableFrom(paramGenericType)) {
                requestParameter.addRequestParameter(parameterName, ConversionUtils.conversion(parameterValue, Resource[].class));
                return true;
            }
        }
        return false;
    }

    public static TempPair<String, ParamLocation> getParamNameAndLocation(Field field, ParamLocation defLocation) {
        HttpParam httpParam = AnnotationUtils.findMergedAnnotation(field, HttpParam.class);
        if (httpParam == null) {
            return TempPair.of(field.getName(), defLocation);
        }
        ParamLocation location = httpParam.location();
        String name = StringUtils.hasText(httpParam.name()) ? httpParam.name() : field.getName();
        return TempPair.of(name, location);
    }

    private static String getParameterName(Parameter parameter, String name) {
        HttpParam httpParamAnn = AnnotationUtils.findMergedAnnotation(parameter, HttpParam.class);
        if (httpParamAnn != null && StringUtils.hasText(httpParamAnn.name())) {
            return httpParamAnn.name();
        }
        return StringUtils.hasText(name) ? name : parameter.getName();
    }

    private static <T extends Annotation> T getAnnotation(Class<?> aClass, Method method, Class<T> annotationType) {
        T annotation = AnnotationUtils.findMergedAnnotation(method, annotationType);
        if (annotation == null) {
            annotation = AnnotationUtils.findMergedAnnotation(aClass, annotationType);
        }
        return annotation;
    }

    private static String getUrlTemp(HttpRequestConfigContent configContent, HttpRequest httpRequestAnn) {
        String urlPrefix = (configContent != null && StringUtils.hasText(configContent.getUrlPrefix())) ? configContent.getUrlPrefix() : "";
        String urlSuffix = (httpRequestAnn != null && StringUtils.hasText(httpRequestAnn.url())) ? httpRequestAnn.url() : "";
        return StringUtils.joinUrlPath(urlPrefix, urlSuffix);
    }

    private static Request getRequest(HttpRequest httpRequestAnn,
                                      HttpRequestConfigContent configContent,
                                      HttpHeaderManager headerManager,
                                      RequestParameter requestParameter) {
        String urlTemp = getUrlTemp(configContent, httpRequestAnn);
        Request request = Request.builder(urlTemp, httpRequestAnn.method());

        if (configContent != null) {
            request.setConnectTimeout(configContent.getConnectTimeout());
            request.setReadTimeout(configContent.getReadTimeout());
            request.setWriterTimeout(configContent.getWriteTimeout());

            for (KV kv : configContent.getCommonHeaders()) {
                request.addHeader(kv.name(), kv.value());
            }

            for (KV kv : configContent.getCommonQueryParams()) {
                request.addQueryParameter(kv.name(), kv.value());
            }

            for (KV kv : configContent.getCommonPathParams()) {
                request.addPathParameter(kv.name(), kv.value());
            }
        }

        request.setHeaders(headerManager.getHeaderMap());
        request.setRequestParameter(requestParameter.getRequestParameters());
        request.setQueryParameters(requestParameter.getQueryParameters());
        request.setPathParameter(requestParameter.getPathParameters());
        BodyObject body = requestParameter.getBody();

        if (body != null) {
            request.setBody(requestParameter.getBody());
        }

        return request;
    }

    public static class HttpRequestConfigContent {

        private String urlPrefix = "";
        private int connectTimeout = 60 * 1000;
        private int readTimeout = 20 * 1000;
        private int writeTimeout = 20 * 1000;
        private KV[] commonHeaders = new KV[0];
        private KV[] commonQueryParams = new KV[0];
        private KV[] commonPathParams = new KV[0];
        private RequestProcessor requestProcessor;
        private SaveResultResponseProcessor responseProcessor;
        private StringResultConvert stringConvert;
        private BytesResultConvert bytesConvert;

        public HttpRequestConfigContent(HttpConfiguration classConfigAnn, HttpConfiguration methodConfigAnn) {
            if (classConfigAnn == null && methodConfigAnn != null) {
                initByOneAnn(methodConfigAnn);
            } else if (classConfigAnn != null && methodConfigAnn == null) {
                initByOneAnn(classConfigAnn);
            } else if (classConfigAnn != null) {
                initByTwoAnn(classConfigAnn, methodConfigAnn);
            }
        }

        private void initByOneAnn(HttpConfiguration confAnn) {
            this.urlPrefix = confAnn.path();
            this.connectTimeout = confAnn.connectTimeout();
            this.readTimeout = confAnn.readTimeout();
            this.writeTimeout = confAnn.writeTimeout();
            this.commonHeaders = confAnn.commonHeaders();
            this.commonQueryParams = confAnn.commonQueryParams();
            this.commonPathParams = confAnn.commonPathParams();
            this.responseProcessor = ClassUtils.newObject(confAnn.responseProcessor());
            this.requestProcessor = RequestProcessor.class == confAnn.requestProcessor() ? null : ClassUtils.newObject(confAnn.requestProcessor());
            this.stringConvert = StringResultConvert.class == confAnn.stringResultConvert() ? null : ClassUtils.newObject(confAnn.stringResultConvert());
            this.bytesConvert = BytesResultConvert.class == confAnn.bytesResultConvert() ? null : ClassUtils.newObject(confAnn.bytesResultConvert());
        }

        private void initByTwoAnn(HttpConfiguration classConfigAnn, HttpConfiguration methodConfigAnn) {
            String mUrlPrefix = methodConfigAnn.path();
            int mConnectTimeout = methodConfigAnn.connectTimeout();
            int mRadTimeout = methodConfigAnn.readTimeout();
            int mWriteTimeout = methodConfigAnn.writeTimeout();
            KV[] mCommonHeader = methodConfigAnn.commonHeaders();
            KV[] mCommonQueryParams = methodConfigAnn.commonQueryParams();
            KV[] commonPathParams = methodConfigAnn.commonPathParams();
            Class<? extends SaveResultResponseProcessor> mRepsProcessorClass = methodConfigAnn.responseProcessor();
            Class<? extends RequestProcessor> mReqProcessorClass = methodConfigAnn.requestProcessor();
            Class<? extends StringResultConvert> mStringConvertClass = methodConfigAnn.stringResultConvert();
            Class<? extends BytesResultConvert> mBytesConvertClass = methodConfigAnn.bytesResultConvert();

            this.urlPrefix = this.urlPrefix.equals(mUrlPrefix) ? classConfigAnn.path() : mUrlPrefix;
            this.connectTimeout = this.connectTimeout == mConnectTimeout ? classConfigAnn.connectTimeout() : mConnectTimeout;
            this.readTimeout = this.readTimeout == mRadTimeout ? classConfigAnn.readTimeout() : mRadTimeout;
            this.writeTimeout = this.writeTimeout == mWriteTimeout ? classConfigAnn.writeTimeout() : mWriteTimeout;
            this.commonHeaders = mCommonHeader.length == 0 ? classConfigAnn.commonHeaders() : mCommonHeader;
            this.commonQueryParams = mCommonQueryParams.length == 0 ? classConfigAnn.commonQueryParams() : mCommonQueryParams;
            this.commonPathParams = commonPathParams.length == 0 ? classConfigAnn.commonPathParams() : commonPathParams;

            Class<? extends SaveResultResponseProcessor> processorClass = mRepsProcessorClass == SaveResultResponseProcessor.class ? classConfigAnn.responseProcessor() : mRepsProcessorClass;
            this.responseProcessor = ClassUtils.newObject(processorClass);

            Class<? extends RequestProcessor> requestProcessorClass = mReqProcessorClass == RequestProcessor.class ? classConfigAnn.requestProcessor() : mReqProcessorClass;
            this.requestProcessor = requestProcessorClass == RequestProcessor.class ? null : ClassUtils.newObject(requestProcessorClass);

            Class<? extends StringResultConvert> stringConvertClass = mStringConvertClass == StringResultConvert.class ? classConfigAnn.stringResultConvert() : mStringConvertClass;
            this.stringConvert = stringConvertClass == StringResultConvert.class ? null : ClassUtils.newObject(stringConvertClass);

            Class<? extends BytesResultConvert> bytesConvertClass = mBytesConvertClass == BytesResultConvert.class ? classConfigAnn.bytesResultConvert() : mBytesConvertClass;
            this.bytesConvert = bytesConvertClass == BytesResultConvert.class ? null : ClassUtils.newObject(bytesConvertClass);

        }

        public String getUrlPrefix() {
            return urlPrefix;
        }

        public void setUrlPrefix(String urlPrefix) {
            this.urlPrefix = urlPrefix;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public KV[] getCommonHeaders() {
            return commonHeaders;
        }

        public void setCommonHeaders(KV[] commonHeaders) {
            this.commonHeaders = commonHeaders;
        }

        public KV[] getCommonQueryParams() {
            return commonQueryParams;
        }

        public void setCommonQueryParams(KV[] commonQueryParams) {
            this.commonQueryParams = commonQueryParams;
        }

        public KV[] getCommonPathParams() {
            return commonPathParams;
        }

        public void setCommonPathParams(KV[] commonPathParams) {
            this.commonPathParams = commonPathParams;
        }

        public SaveResultResponseProcessor getResponseProcessor() {
            return responseProcessor;
        }

        public void setResponseProcessor(SaveResultResponseProcessor responseProcessor) {
            this.responseProcessor = responseProcessor;
        }

        public StringResultConvert getStringConvert() {
            return stringConvert;
        }

        public void setStringConvert(StringResultConvert stringConvert) {
            this.stringConvert = stringConvert;
        }

        public BytesResultConvert getBytesConvert() {
            return bytesConvert;
        }

        public void setBytesConvert(BytesResultConvert bytesConvert) {
            this.bytesConvert = bytesConvert;
        }

        public RequestProcessor getRequestProcessor() {
            return requestProcessor;
        }

        public void setRequestProcessor(RequestProcessor requestProcessor) {
            this.requestProcessor = requestProcessor;
        }
    }
}
