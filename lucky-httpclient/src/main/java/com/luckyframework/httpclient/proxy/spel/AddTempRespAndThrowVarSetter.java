package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.spel.LazyValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
 * 用于添加临时{@link Response}以及{@link Throwable}变量的{@link ParamWrapperSetter}
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/23 01:33
 */
public class AddTempRespAndThrowVarSetter implements ParamWrapperSetter {

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
     * 额外参数消费者，用于扩展，外部可以通过此属性来注入更多的变量
     */
    private Consumer<Map<String, Object>> extendMapConsumer;

    /**
     * 构造器
     *
     * @param response  临时响应对象
     * @param context   上下文对象
     * @param throwable 异常对象
     */
    public AddTempRespAndThrowVarSetter(@Nullable Response response, @NonNull Context context, @Nullable Throwable throwable) {
        this.response = response;
        this.throwable = throwable;
        this.context = context;
    }

    /**
     * 设置额外参数消费者
     *
     * @param extendMapConsumer 额外参数消费者
     */
    public void setExtendMapConsumer(Consumer<Map<String, Object>> extendMapConsumer) {
        this.extendMapConsumer = extendMapConsumer;
    }

    @Override
    public void setting(MutableMapParamWrapper paramWrapper) {
        Map<String, Object> extendMap = new ConcurrentHashMap<>(11);
        if (throwable != null) {
            extendMap.put($_THROWABLE_$, LazyValue.of(throwable));
        }
        if (response != null) {
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
        }

        applyExtendMapConsumer(extendMap);

        paramWrapper.getRootObject().addFirst(extendMap);
    }

    /**
     * 应用消费
     * @param extendMap 额外参数Map
     */
    private void applyExtendMapConsumer(Map<String, Object> extendMap) {
        if (this.extendMapConsumer != null) {
            this.extendMapConsumer.accept(extendMap);
        }
    }

}
