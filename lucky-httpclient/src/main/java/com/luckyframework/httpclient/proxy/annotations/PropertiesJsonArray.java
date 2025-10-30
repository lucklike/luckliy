package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.setter.JsonArrayBodyFactoryParameterSetter;
import com.luckyframework.httpclient.proxy.statics.PropertiesJsonArrayResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态JSON对象配置注解,支持使用properties文件格式来配置一个JSON数组
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 13:57
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticParam(
        setter = @ObjectGenerate(clazz = JsonArrayBodyFactoryParameterSetter.class, scope = Scope.METHOD),
        resolver = @ObjectGenerate(PropertiesJsonArrayResolver.class)
)
@Combination(StaticParam.class)
public @interface PropertiesJsonArray {

    /**
     * <pre>
     * 支持使用properties文件格式来配置一个JSON数组
     * 默认格式为：key=value，注：key必须以<b>prefix[n]</b>开头
     * key和value部分均支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * {@code
     * 1.配置普通元素：
     *  $[0]=123
     *  $[1]=456
     *  ==>
     *  [
     *      123,
     *      456
     *  ]
     *
     * 2.配置对象元素：
     *  $[0].object.key1=one
     *  $[0].object.key2=two
     *  $[1].object.key1=one
     *  $[1].object.key2=two
     *  ==>
     *  [
     *      {
     *          "object": {
     *              "key1": "one",
     *              "key2": "two"
     *          }
     *      },
     *      {
     *          "object": {
     *              "key1": "one",
     *              "key2": "two"
     *           }
     *
     *      }
     *  ]
     *
     *  3.多维数组
     *  $[0][0]=1
     *  $[0][1]=2
     *  $[1][0]=3
     *  $[1][1]=4
     *  ==>
     *  [[1,2],[3,4]]
     * </pre>
     * }
     *
     * @see SpELVariableNote
     */
    String[] value();

    /**
     * 属性名与属性值之间的分隔符
     */
    String separator() default "=";

    /**
     * 数组前缀
     */
    String prefix() default "\\$";

}
