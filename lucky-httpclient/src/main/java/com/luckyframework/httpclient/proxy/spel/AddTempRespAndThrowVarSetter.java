package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.spel.LazyValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getConvertMetaType;
import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CONTENT_LENGTH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CONTENT_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_BYTE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STATUS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STREAM_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STRING_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_THROWABLE_$;

/**
 * 用于添加临时{@link Response}以及{@link Throwable}变量的{@link ContextSpELExecution.ParamWrapperSetter}
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/23 01:33
 */
public class AddTempRespAndThrowVarSetter implements ContextSpELExecution.ParamWrapperSetter {

    /**
     * 临时响应对象
     */
    private final Response response;

    /**
     * 异常对象
     */
    private final Throwable throwable;

    /**
     * 上下文对象
     */
    private final Context context;

    /**
     * 构造器
     *
     * @param response  临时响应对象
     * @param context   上下文对象
     * @param throwable 异常对象
     */
    public AddTempRespAndThrowVarSetter(@NonNull Response response, @NonNull Context context, @Nullable Throwable throwable) {
        this.response = response;
        this.throwable = throwable;
        this.context = context;
    }

    @Override
    public void setting(MutableMapParamWrapper paramWrapper) {
        Map<String, Object> extendMap = new ConcurrentHashMap<>(11);
        if (throwable != null) {
            extendMap.put($_THROWABLE_$, LazyValue.of(throwable));
        }
        extendMap.put($_RESPONSE_$, LazyValue.of(response));
        extendMap.put($_RESPONSE_STATUS_$, LazyValue.of(response::getStatus));
        extendMap.put($_CONTENT_LENGTH_$, LazyValue.of(response::getContentLength));
        extendMap.put($_CONTENT_TYPE_$, LazyValue.of(response::getContentType));
        extendMap.put($_RESPONSE_HEADER_$, LazyValue.of(response::getSimpleHeaders));
        extendMap.put($_RESPONSE_COOKIE_$, LazyValue.of(response::getSimpleCookies));
        extendMap.put($_RESPONSE_STREAM_BODY_$, LazyValue.rtc(response::getInputStream));
        extendMap.put($_RESPONSE_STRING_BODY_$, LazyValue.of(response::getStringResult));
        extendMap.put($_RESPONSE_BYTE_BODY_$, LazyValue.of(response::getResult));
        extendMap.put($_RESPONSE_BODY_$, LazyValue.of(() -> getResponseBody(response, () -> getConvertMetaType(context))));
        paramWrapper.getRootObject().addFirst(extendMap);
    }

}
