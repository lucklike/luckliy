package com.luckyframework.httpclient.proxy.spel;


import com.luckyframework.common.ExpressionBean;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.spel.ParamWrapper;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Objects;

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

    /**
     * 私有构造函数
     *
     * @param mc   方法上下文
     * @param bean Bean对象
     */
    private SpelBean(MethodContext mc, T bean) {
        this.mc = mc;
        this.bean = bean;
    }

    /**
     * 获取一个{@link SpelBean}实例
     *
     * @param mc   方法上下文
     * @param bean Bean对象
     * @param <T>  Bean类型
     * @return {@link SpelBean}实例
     */
    public static <T> SpelBean<T> of(MethodContext mc, T bean) {
        return new SpelBean<>(mc, bean);
    }

    /**
     * 获取原始Bean对象
     *
     * @return 原始Bean对象
     */
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
        ParamWrapper beanParamWrapper = ParamWrapper.craft(mc.getFinallyVar());
        beanParamWrapper.setExpression(expression);
        beanParamWrapper.setExpectedResultType(type);
        beanParamWrapper.setRootObject(bean);
        return mc.getSpELConverter().getSpELRuntime().getValueForType(beanParamWrapper);
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

    public boolean eq(String expression, Object value) {
        return Objects.equals(get(expression), value);
    }

    public boolean ne(String expression, Object value) {
        return !eq(expression, value);
    }

    public <V> boolean eq(String expression, V value, Class<V> type) {
        return Objects.equals(get(expression, type), value);
    }

    public <V> boolean ne(String expression, V value, Class<V> type) {
        return !eq(expression, value, type);
    }
}
