package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.setter.JsonObjectBodyFactoryParameterSetter;
import com.luckyframework.httpclient.proxy.statics.PropertiesJsonObjectResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态JSON对象配置注解,支持使用properties文件格式来配置一个JSON对象
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
        setter = @ObjectGenerate(JsonObjectBodyFactoryParameterSetter.class),
        resolver = @ObjectGenerate(PropertiesJsonObjectResolver.class)
)
@Combination(StaticParam.class)
public @interface PropertiesJson {

    /**
     * <pre>
     * 支持使用properties文件格式来配置一个JSON对象
     * 默认格式为：key=value，
     * key和value部分均支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * {@code
     * 1.配置数组：
     *  array[0]=123
     *  array[1]=456
     *  ==>
     * {
     *     "array": [123, 456]
     * }
     *
     * 2.配置对象：
     *  object.key1=one
     *  object.key2=two
     *  ==>
     *  {
     *      "object": {
     *          "key1": "one",
     *          "key2": "two"
     *      }
     *  }
     *
     * 3.复杂对象：
     *  obj1.key1.users[0].id=123
     *  obj1.key1.users[0].name=USER-1
     *  obj1.key1.users[1].id=456
     *  obj1.key1.users[1].name=USER-2
     *  obj1.key2.books[0].id=987
     *  obj1.key2.books[0].book=BOOK-1
     *  obj1.key2.books[1].id=444
     *  obj1.key2.books[1].book=BOOK-2
     *  ==>
     *  {
     *      "obj1": {
     *          "key1": {
     *              "users": [
     *                  {
     *                      "id": 123,
     *                      "name": "USER-1"
     *                  },
     *                  {
     *                      "id": 456,
     *                      "name": "USER-2"
     *                  }
     *              ]
     *          },
     *          "key2": {
     *              "books": [
     *                  {
     *                      "id": 987,
     *                      "book": "BOOK-1"
     *                  },
     *                  {
     *                      "id": 444,
     *                      "book": "BOOK-2"
     *                  }
     *              ]
     *          }
     *      }
     *  }
     * }
     * </pre>
     *
     *
     * <pre>
     * SpEL表达式内置参数有：
     *  root:{
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *  }
     * </pre>
     */
    String[] value();

    /**
     * 属性名与属性值之间的分隔符
     */
    String separator() default "=";

}
