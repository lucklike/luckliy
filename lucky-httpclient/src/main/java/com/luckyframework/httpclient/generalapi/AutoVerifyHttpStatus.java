package com.luckyframework.httpclient.generalapi;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Objects;

/**
 * 自动检验HTTP状态码
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/14 23:14
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpELImport(AutoVerifyHttpStatus.Check.class)
public @interface AutoVerifyHttpStatus {

    String errMsg() default "";

    /**
     * 定义正常的状态码
     */
    int[] normal() default {};

    /**
     * 定义异常的状态码
     */
    int[] err() default {};

    /**
     * 定义正常状态码的表达式
     */
    String condition() default "";

    /**
     * 包含一个校验HTTP状态码回调函数的工具类
     */
    class Check {

        /**
         * 校验HTTP状态码的回调函数
         *
         * @param mc       方法上下文
         * @param response 响应对象
         */
        @Callback(lifecycle = Lifecycle.RESPONSE)
        private static void onCheck(MethodContext mc, Response response) {
            int status = response.getStatus();
            AutoVerifyHttpStatus ann = mc.getMergedAnnotationCheckParent(AutoVerifyHttpStatus.class);
            Boolean isNormal;
            String conditionStr;

            String condition = ann.condition();
            if (StringUtils.hasText(condition)) {
                isNormal = mc.parseExpression(condition, Boolean.class);
                conditionStr = condition + " Is False";
            } else if (ann.err().length > 0) {
                isNormal = ContainerUtils.notInArrays(ConversionUtils.conversion(ann.err(), Integer[].class), status);
                conditionStr = " $status$ in ErrStatus" + Arrays.toString(ann.err());
            } else if (ann.normal().length > 0) {
                conditionStr = " $status$ not in NormalStatus" + Arrays.toString(ann.normal());
                isNormal = ContainerUtils.inArrays(ConversionUtils.conversion(ann.normal(), Integer[].class), status);
            } else {
                conditionStr = " $status$ >= 400 ";
                isNormal = !HttpStatus.err(status);
            }

            // 不正常的情况需要进行异常处理
            if (Objects.equals(isNormal, Boolean.FALSE)) {
                String message;
                if (StringUtils.hasText(ann.errMsg())) {
                    message = mc.parseExpression(ann.errMsg(), String.class);
                } else if (response.getContentLength() != 0 && StringUtils.hasText(response.getStringResult())) {
                    message = response.getStringResult();
                } else if (HttpStatus.getStatus(status) != null) {
                    message = HttpStatus.getStatus(status).getDesc();
                } else {
                    message = "Http status exception.";
                }

                String tag = FontUtil.getBackRedStr(" HTTP STATUS EXCEPTION ");
                throw new HttpStatusException(
                        "  \n\t{}\n\t❓ {}  👉 {}\n\t❌ {} 👉 {}\n\t❌ {} 👉 {}{}\n\t❌ {} 👉 {} \n\t{}",
                        tag,
                        FontUtil.getWhiteStr("Condition"),
                        FontUtil.getMulberryUnderline(conditionStr),
                        FontUtil.getWhiteStr("Status    "),
                        FontUtil.getRedStr(String.valueOf(status)),
                        FontUtil.getWhiteStr("Url       "),
                        FontUtil.getYellowStr("["+response.getRequest().getRequestMethod().toString()+"] "),
                        FontUtil.getYellowUnderline(response.getRequest().getUrl()),
                        FontUtil.getWhiteStr("Message   "),
                        FontUtil.getRedStr(message.replace("\n", "").replace("\r", "").replace("\t", "")),
                        tag
                );
            }
        }
    }
}
