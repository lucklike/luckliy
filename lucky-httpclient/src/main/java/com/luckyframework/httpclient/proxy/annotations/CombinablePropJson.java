package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.setter.FlatBeanParameterSetter;
import com.luckyframework.httpclient.proxy.statics.FlatBeanPropertiesJsonObjectResolver;
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
 * @see CombineJson
 * @see JsonParam
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticParam(
        setter = @ObjectGenerate(FlatBeanParameterSetter.class),
        resolver = @ObjectGenerate(FlatBeanPropertiesJsonObjectResolver.class)
)
@Combination(StaticParam.class)
public @interface CombinablePropJson {

    /**
     * <pre>
     * 支持使用properties文件格式来配置一个JSON对象
     * 默认格式为：key=value，
     * key和value部分均支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * {@code
     * 1.配置数组：
     *  [0]=123
     *  [1]=456
     *  ==>
     *  [
     *      123,
     *      456
     *  ]
     *
     * 2.配置对象元素：
     *  [0].object.key1=one
     *  [0].object.key2=two
     *  [1].object.key1=one
     *  [1].object.key2=two
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
     *  [0][0]=1
     *  [0][1]=2
     *  [1][0]=3
     *  [1][1]=4
     *  ==>
     *  [[1,2],[3,4]]
     *
     * 4.配置对象：
     *  object.key1=one
     *  object.key2=two
     *  ['object']['key3']=three
     *  ==>
     *  {
     *      "object": {
     *          "key1": "one",
     *          "key2": "two",
     *          "key3": "three",
     *      }
     *  }
     *
     * 5.复杂对象：
     *  obj1.key1.users[0].id=123
     *  obj1.key1.users[0].name=USER-1
     *  obj1.key1.users[1].id=456
     *  obj1.key1.users[1].name=USER-2
     *  obj1.key2.books[0].id=987
     *  obj1.key2.books[0].book=BOOK-1
     *  obj1.key2.books[1].id=444
     *  ['obj1']['key2']['books'][1]['book']=BOOK-2
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
     *
     * }
     * </pre>
     *
     * @see SpELVariableNote
     */
    String[] value();

    /**
     * 属性名与属性值之间的分隔符
     */
    String separator() default "=";

}
