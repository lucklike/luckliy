package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.convert.ResultSelectionResponseConvert;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应结果转换注解
 *
 * <pre>
 *     <b>SpEL表达式用法:</b><br/><br/>
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
 *          扩展：.^[]选取满足要求的第一个元素， .$[]选取满足要求的最后一个
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
 *
 * </pre>}
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(ResultConvertMeta.class)
@ResultConvertMeta(convert = @ObjectGenerate(ResultSelectionResponseConvert.class))
public @interface RespConvert {

    /**
     * 同{@link #result()}
     */
    @AliasFor("result")
    String value() default "";

    /**
     * 取值表达式，使用SpEL表达式来将Response转化为方法返回值，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
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
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
     */
    String result() default "";

    /**
     * 指定响应类型转换的SpEL函数
     */
    String resultFunc() default "";

    /**
     * 配置了该属性则会抛出携带该异常信息的异常，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
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
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
     */
    String exception() default "";

    /**
     * 转换元类型
     */
    @AliasFor(annotation = ResultConvertMeta.class, attribute = "metaType")
    Class<?> metaType() default Object.class;
}
