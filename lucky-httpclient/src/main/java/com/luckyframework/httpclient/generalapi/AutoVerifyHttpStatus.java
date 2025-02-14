package com.luckyframework.httpclient.generalapi;

import com.luckyframework.common.Console;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.logging.FontUtil;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
        private static void check(MethodContext mc, Response response) {
            int status = response.getStatus();
            if (HttpStatus.err(status)) {

                AutoVerifyHttpStatus ann = mc.getMergedAnnotationCheckParent(AutoVerifyHttpStatus.class);
                String err = HttpStatus.getStatus(status).getDesc();
                if (StringUtils.hasText(ann.errMsg())) {
                    err = mc.parseExpression(ann.errMsg(), String.class);
                }

                throw new HttpStatusException(
                        "Http Status Error! {}{} - {}{} [{}] {}",
                        FontUtil.getWhiteStr("##"),
                        Console.getRedString("<" + status + ">"),
                        err,
                        FontUtil.getWhiteStr("##"),
                        Console.getYellowString(response.getRequest().getRequestMethod()),
                        response.getRequest().getUrl()
                );
            }
        }
    }
}
