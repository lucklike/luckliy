package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.exeception.ConvertException;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CONTENT_LENGTH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CONTENT_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_FORM_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_METHOD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_PATH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_QUERY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_URL_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_URL_PATH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_BYTE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STATUS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STREAM_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STRING_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$CONVERT_META_TYP$__;

/**
 * SpEl变量管理器的默认实现
 */
public class DefaultSpELVarManager implements SpELVarManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultSpELVarManager.class);

    /**
     * 上下文SpEL变量
     */
    private final SpELVariate spELVariate;

    public DefaultSpELVarManager(Context context) {
        this.spELVariate = new SpELVariate(context);
    }

    @NonNull
    @Override
    public SpELVariate getContextVar() {
        return this.spELVariate;
    }

    @Override
    public void setRequestVar(Request request) {
        Map<String, Object> immutableMap = new HashMap<>(16);
        immutableMap.put($_REQUEST_$, LazyValue.of(request));
        immutableMap.put($_REQUEST_URL_$, LazyValue.rtc(request::getUrl));
        immutableMap.put($_REQUEST_URL_PATH_$, LazyValue.rtc(() -> request.getURL().getPath()));
        immutableMap.put($_REQUEST_METHOD_$, LazyValue.rtc(request::getRequestMethod));
        immutableMap.put($_REQUEST_QUERY_$, LazyValue.rtc(request::getSimpleQueries));
        immutableMap.put($_REQUEST_PATH_$, LazyValue.rtc(request::getPathParameters));
        immutableMap.put($_REQUEST_FORM_$, LazyValue.rtc(request::getFormParameters));
        immutableMap.put($_REQUEST_HEADER_$, LazyValue.rtc(request::getSimpleHeaders));
        immutableMap.put($_REQUEST_COOKIE_$, LazyValue.rtc(request::getSimpleCookies));

        spELVariate.addRootVariable(ValueSpaceConstant.REQUEST_SPACE, Collections.unmodifiableMap(immutableMap));
    }


    @Override
    public void setResponseVar(Response response, Context context) {
        Map<String, Object> immutableMap = new HashMap<>(16);
        immutableMap.put($_RESPONSE_$, LazyValue.of(response));
        immutableMap.put($_RESPONSE_STATUS_$, LazyValue.of(response::getStatus));
        immutableMap.put($_CONTENT_LENGTH_$, LazyValue.of(response::getContentLength));
        immutableMap.put($_CONTENT_TYPE_$, LazyValue.of(response::getContentType));
        immutableMap.put($_RESPONSE_HEADER_$, LazyValue.of(response::getSimpleHeaders));
        immutableMap.put($_RESPONSE_COOKIE_$, LazyValue.of(response::getSimpleCookies));
        immutableMap.put($_RESPONSE_STREAM_BODY_$, LazyValue.rtc(response::getInputStream));
        immutableMap.put($_RESPONSE_STRING_BODY_$, LazyValue.of(response::getStringResult));
        immutableMap.put($_RESPONSE_BYTE_BODY_$, LazyValue.of(response::getResult));
        immutableMap.put($_RESPONSE_BODY_$, LazyValue.of(() -> getResponseBody(response, () -> getConvertMetaType(context))));

        spELVariate.addRootVariable(ValueSpaceConstant.RESPONSE_SPACE, Collections.unmodifiableMap(immutableMap));
    }

    public static Class<?> getConvertMetaType(Context context) {
        Object var = context.getVar(__$CONVERT_META_TYP$__);
        if (var == null) {
            return context.getConvertMetaType();
        }
        if (var instanceof Class) {
            return (Class<?>) var;
        }
        throw new ConvertException("Failed to obtain the meta type. Please check whether the built-in variable {} value type is correct", __$CONVERT_META_TYP$__);
    }


    public static Object getResponseBody(Response response, Supplier<Class<?>> metaTypeSupplier) {
        Class<?> metaType = metaTypeSupplier.get();
        try {
            Object entity = response.getEntity(metaType);
            return entity == null ? response.getStringResult() : entity;
        } catch (Exception e) {
            log.warn("The response body cannot be converted to the specified '{}' type, and the response result will be stored in the SpEL runtime environment as a String", metaType);
            return response.getStringResult();
        }
    }

}
