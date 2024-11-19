package com.luckyframework.httpclient.proxy.spel.pack;

import com.luckyframework.httpclient.proxy.spel.var.VarScope;
import com.luckyframework.httpclient.proxy.spel.var.VarType;
import com.luckyframework.httpclient.proxy.spel.var.Variate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 导入包
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:58
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Variate(scope = VarScope.CLASS, type = VarType.NORMAL)
public @interface Package {

    VarScope scope() default VarScope.DEFAULT;
}
