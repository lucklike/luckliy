package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.convert.SpELResponseSelectConvert;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 支持SpEL表达式的响应结果转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ResultConvert(convert = SpELResponseSelectConvert.class)
public @interface SpElSelect {

    /**
     * <pre>
     *     对响应结果进行操作的SpEL表达式，<b>SpEL表达式部分需要写在#{}中</b>
     *     1.<a href="https://docs.spring.io/spring-framework/reference/core/expressions.htm">Spring Expression Language Official Document</a>
     *
     *     2.Elvis运算符
     *     {@code
     *          x?:y
     *          ->
     *          x != null ? x : y
     *     }
     *
     *     3.安全导航操作符（避免空指针异常）
     *     {@code
     *          object?.field
     *          ->
     *          当object为null时会直接返回null（不产生空指针异常），不为null时返回object的field属性
     *     }
     *
     *     4.集合选择器语法，对集合或Map进行过滤
     *       {@code
     *          [collection | array | map].?[selectorExpression]
     *          eg:
     *          a.  int[] array = {1,2,3,4,5,6,7,8,9,10}
     *              array.?[#this % 2 == 0]
     *                     ->
     *              {2,4,6,8,10}
     *
     *          b. Map map = {a=1,b=2,c=3,d=4,e=5,f=6,g=7,h=8,i=9,j=10,k=11,l=12}
     *             map.?[value > 5 && (key eq 'a' || key eq 'f' || key eq 'j')]
     *                     ->
     *             {f=6, j=10}
     *       }
     *     5.集合投影语法，对集合或Map的元素进行操作，生成一个新集合
     *       {@code
     *          [collection | array | map].![expression]
     *          eg:
     *           a.  int[] array = {1,2,3,4,5,6,7,8,9,10}
     *               array.![#this + 1]
     *                      ->
     *               {2,3,4,5,6,7,8,9,10,11}
     *
     *           b.  Map map = {a=1,b=2,c=3,d=4,e=5,f=6,g=7,h=8,i=9,j=10,k=11,l=12}
     *               map.![value+1]
     *                      ->
     *               [2,3,4,5,6,7,8,9,10,11,12,13]
     *
     *           c. List<Map> list = [{a=1,b=2,c=3}]
     *              list.![{'A':a, 'B':b, 'C':c}]
     *                      ->
     *              [{A=1,B=2,C=3}]
     *       }
     * </pre>
     *
     */
    @AliasFor("expression")
    String value() default "";

    /**
     * 同value
     */
    @AliasFor("value")
    String expression() default "";

    /**
     * 当取值表达式取不到值时可以通过这个属性来设置默认值，
     * 这里允许使用SpEL表达式来生成一个默认值，<b>SpEL表达式部分需要写在#{}中</b>
     */
    String defaultValue() default "";

    /**
     * 异常信息，当从条件表达式中无法获取值时又没有设置默认值时
     * 配置了该属性则会抛出携带该异常信息的异常，
     * 这里允许使用SpEL表达式来生成一个默认值，<b>SpEL表达式部分需要写在#{}中</b>
     */
    String exMsg() default "";
}
