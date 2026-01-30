package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.function.CommonFunctions;
import com.luckyframework.spel.LazyValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
            extendMap.putAll(CommonFunctions.sta(response, context));
        }
        applyExtendMapConsumer(extendMap);

        paramWrapper.getRootObject().addFirst(Collections.singletonMap(ValueSpaceConstant.RESPONSE_SPACE, extendMap));
    }

    /**
     * 应用消费
     *
     * @param extendMap 额外参数Map
     */
    private void applyExtendMapConsumer(Map<String, Object> extendMap) {
        if (this.extendMapConsumer != null) {
            this.extendMapConsumer.accept(extendMap);
        }
    }

}
