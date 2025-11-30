package com.luckyframework.httpclient.proxy.spel;


import com.luckyframework.common.ExpressionBean;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Collections;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_BEAN_$;

/**
 * 支持使用 SpEL 表达式操作的 Bean 对象
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/30 17:37
 */
public class SpelBean<T> implements ExpressionBean<T> {

    /**
     * 方法上下文
     */
    private final MethodContext mc;

    /**
     * Bean 对象
     */
    private final T bean;

    private SpelBean(MethodContext mc, T bean) {
        this.mc = mc;
        this.bean = bean;
    }

    public static <T> SpelBean<T> of(MethodContext mc, T bean) {
        return new SpelBean<>(mc, bean);
    }

    @Override
    public T getBean() {
        return bean;
    }

    @Override
    public void set(String expression, Object value) {
        mc.getSpELConverter().getSpELRuntime().setValue(bean, expression, value);
    }

    @Override
    public <V> V get(String expression, Type type) {
        MutableMapParamWrapper mmpw = mc.getFinallyVar();
        mmpw.setExpression(expression);
        mmpw.setExpectedResultType(type);
        mmpw.getRootObject().addFirst(Collections.singletonMap($_BEAN_$, bean));
        return mc.getSpELConverter().getSpELRuntime().getValueForType(mmpw);
    }

    public <V> SpelBean<V> getSpelBean(String expression, Type type) {
        return SpelBean.of(mc, get(expression, type));
    }

    public <V> SpelBean<V> getSpelBean(String expression, Class<V> type) {
        return getSpelBean(expression, (Type) type);
    }

    public <V> SpelBean<V> getSpelBean(String expression, SerializationTypeToken<V> type) {
        return getSpelBean(expression, type.getType());
    }

    public <V> SpelBean<V> getSpelBean(String expression, ResolvableType type) {
        return getSpelBean(expression, type.getType());
    }

    public SpelBean<?> getSpelBean(String expression) {
        return getSpelBean(expression, Object.class);
    }

}
