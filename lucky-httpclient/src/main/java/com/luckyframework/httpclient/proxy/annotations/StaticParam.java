package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 *    静态参数配置注解，为程序提供个性化静态参数配置扩展的能力，开发自己的静态参数组件必须遵守以下原则：
 *    1.自定义的注解必须被@StaticParam注解标注
 *    2.自定义注解必须定义以下四个属性：
 *          // 参数设置器，定义如何将参数设置到请求体中
 *          Class<? extends ParameterSetter> paramSetter() default ParameterSetter.class;
 *          // 参数设置器的额外描述信息，用于后续扩展
 *          String paramSetterMsg() default "";
 *          // 参数解析器，用于将用户配置再注解中的参数解析成具体的Http参数
 *          Class<? extends StaticParamResolver> paramResolver() default StaticParamResolver.class;
 *          // 参数解析器器的额外描述信息，用于后续扩展
 *          String paramResolverMsg() default "";
 *    3.开发自己的StaticParamResolver组件并设置给paramResolver属性
 *    4.开发自己的ParameterSetter组件并设置给paramResolver属性
 * </pre>
 *
 *
 * @see BasicAuth
 * @see StaticResource
 * @see StaticHeader
 * @see StaticQuery
 * @see StaticForm
 * @see StaticPath
 * @see StaticCookie
 * @see Proxy
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StaticParam {

}
