package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.ConvertMetaData;
import com.luckyframework.httpclient.proxy.exeception.ConvertException;
import com.luckyframework.httpclient.proxy.function.CommonFunctions;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_FORM_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_METHOD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_MULTIPART_FORM_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_PATH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_QUERY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_THREAD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_URL_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_URL_PATH_$;
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
        immutableMap.put($_REQUEST_MULTIPART_FORM_$, LazyValue.rtc(request::getMultipartFormParameters));
        immutableMap.put($_REQUEST_HEADER_$, LazyValue.rtc(request::getSimpleHeaders));
        immutableMap.put($_REQUEST_COOKIE_$, LazyValue.rtc(request::getSimpleCookies));
        immutableMap.put($_REQUEST_THREAD_$, Thread.currentThread());

        spELVariate.addRootVariable(ValueSpaceConstant.REQUEST_SPACE, Collections.unmodifiableMap(immutableMap));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSourceResponseVar(Response response, Context context) {
        if (spELVariate.hasRootVariable(ValueSpaceConstant.RESPONSE_SPACE_SOURCE)) {
            Map<String, Object> sourceRespVarMap = (Map<String, Object>) spELVariate.getRoot().get(ValueSpaceConstant.RESPONSE_SPACE_SOURCE);
            sourceRespVarMap.clear();
            sourceRespVarMap.putAll(getResponseVarMap(response, context));
        } else {
            spELVariate.addRootVariable(ValueSpaceConstant.RESPONSE_SPACE_SOURCE, getResponseVarMap(response, context));
        }

    }

    @Override
    public void setResponseVar(Response response, Context context) {
        spELVariate.addRootVariable(ValueSpaceConstant.RESPONSE_SPACE, getResponseVarMap(response, context));
    }


    private Map<String, Object> getResponseVarMap(Response response, Context context) {
        return CommonFunctions.sta(response, context);
    }

    public static ConvertMetaData getConvertMetaType(Context context) {
        Object var = context.getVar(__$CONVERT_META_TYP$__);
        if (var == null) {
            return context.getConvertMetaType();
        }
        if (var instanceof ConvertMetaData) {
            return (ConvertMetaData) var;
        }
        throw new ConvertException("Failed to obtain the meta type. Please check whether the built-in variable {} value type is correct", __$CONVERT_META_TYP$__);
    }


    public static Object getResponseBody(Response response, ConvertMetaData metaData) {
        Object entity = response.getEntity(metaData.getMetaType());
        return entity == null ? response.getStringResult() : entity;
    }

}
