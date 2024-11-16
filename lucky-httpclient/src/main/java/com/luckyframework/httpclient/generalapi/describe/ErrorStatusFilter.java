package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.httpclient.proxy.annotations.Condition;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.var.ResponseRootVar;
import com.luckyframework.httpclient.proxy.spel.var.RootLiteral;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * 错误状态过滤器，拦截响应，如果响应体中的状态码信息异常，则会直接报错
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/16 23:55
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Condition(assertion = "``#{$assert.status}``", exception = "``#{$err.status}``")
@Condition(assertion = "``#{$assert.code}``", exception = "``#{$err.code}``")
@RespConvert
@SpELImport({DescribeFunction.class, CommonErrorMsgVars.class, ErrorStatusFilter.StatusFieldConvert.class})
public @interface ErrorStatusFilter {

    /**
     * 获取接口响应码的SpEL表达式
     */
    String respCodeExp();

    /**
     * 对接口响应码进行断言的SpEL表达式
     */
    String respCodeAssertExp();

    /**
     * 错误HTTP状态码时获取提示信息的SpEL表达式
     */
    String respCodeErrMsgExp() default "";

    /**
     * 错误HTTP状态码时获取提示信息的SpEL表达式
     */
    String statusErrMsgExp() default "";

    /**
     * 对HTTP状态码进行断言的SpEL表达式
     */
    String statusAssertExp() default "";

    /**
     * 定义正常的响应码
     */
    int[] normalStatus() default {};


    class StatusFieldConvert {

        @ResponseRootVar(unfold = true)
        private static final Map<String, Object> _var = new HashMap<String, Object>() {{
            put("_statusExp_", "``#{#_esfAnn_($cc$).statusAssertExp}``");
            put("_normalStatus_", "``#{#_esfAnn_($cc$).normalStatus}``");
        }};

        @RootLiteral(unfold = true)
        private static final Map<String, Object> _literal = new HashMap<String, Object>() {{
            // 必填
            put("__code__", "#{#_esfAnn_($cc$).respCodeExp}");
            put("__respCodeAssertExp__", "#{#_esfAnn_($cc$).respCodeAssertExp}");

            // 选填项
            put("_msg_", "#{#_esfAnn_($cc$).respCodeErrMsgExp}");
            put("_statusErrMsg_", "#{#_esfAnn_($cc$).statusErrMsgExp}");
        }};


        public static ErrorStatusFilter _esfAnn_(ClassContext context) {
            return context.getMergedAnnotation(ErrorStatusFilter.class);
        }
    }
}
