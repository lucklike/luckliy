package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.proxy.impl.ResponseSelectConvert;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应结果转换器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ResultConvert(convert = ResponseSelectConvert.class)
public @interface ResultSelect {

    /**
     * 指定需要获取元素的key
     * <pre>
     * 取值表达式@resp.${key}，请参照{@link ConfigurationMap#getProperty(String)}的用法，
     * 其中@resp为固定的前缀，表示整合响应结果。
     * 从数组中取值：@resp.array[0].user或@resp[1].user.password
     * 从对象中取值：@resp.object.user或@resp.user.password
     * </pre>
     */
    @AliasFor("key")
    String value() default "";

    /**
     * 同value
     */
    @AliasFor("value")
    String key() default "";

    /**
     * 当取值表达式取不到值时可以通过这个属性来设置默认值，
     * 这里允许使用SpEL表达式来生成一个默认值
     */
    String defaultValue() default "";
}
