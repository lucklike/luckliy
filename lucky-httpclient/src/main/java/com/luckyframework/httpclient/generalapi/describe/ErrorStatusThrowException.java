package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.httpclient.proxy.annotations.Condition;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.spel.function.Function;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.var.ResponseRootVar;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * 检测到错误状态码抛异常
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
@SpELImport({DescribeFunction.class, CommonErrorMsgVars.class, ErrorStatusThrowException.StatusFieldConvert.class})
public @interface ErrorStatusThrowException {

    /**
     * 获取接口响应码的SpEL表达式
     */
    String code();

    /**
     * 断言响应码为错误的SpEL表达式
     */
    String errCodeAssert();

    /**
     * 错误HTTP状态码时获取提示信息的SpEL表达式
     */
    String errCodeMsg() default "";

    /**
     * 错误HTTP状态码时获取提示信息的SpEL表达式
     */
    String errStatusMsg() default "";

    /**
     * 断言HTTP状态码为错误的SpEL表达式
     */
    String errStatusAssert() default "";

    /**
     * 定义正常的响应码
     */
    int[] normalStatus() default {};


    class StatusFieldConvert {

        @ResponseRootVar(unfold = true)
        private static final Map<String, Object> _var = new HashMap<String, Object>() {{
            // 必填
            put("__code__", "#{#__esteAnn($cc$).code}");
            put("__respCodeAssertExp__", "#{#__esteAnn($cc$).errCodeAssert}");

            // 选填项
            put("_statusExp_", "``#{#__esteAnn($cc$).errStatusAssert}``");
            put("_normalStatus_", "``#{#__esteAnn($cc$).normalStatus}``");
            put("_msg_", "#{#__esteAnn($cc$).errCodeMsg}");
            put("_statusErrMsg_", "#{#__esteAnn($cc$).errStatusMsg}");
        }};

        @Function("__esteAnn")
        public static ErrorStatusThrowException getErrorStatusThrowExceptionAnn(ClassContext context) {
            return context.getMergedAnnotation(ErrorStatusThrowException.class);
        }
    }
}
