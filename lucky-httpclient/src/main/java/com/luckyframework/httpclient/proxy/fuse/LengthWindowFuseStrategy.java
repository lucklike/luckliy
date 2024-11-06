package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.creator.Scope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@FuseMeta(fuse = @ObjectGenerate(clazz = LengthWindowStatisticsFuseProtector.class, scope = Scope.CLASS))
public @interface LengthWindowFuseStrategy {

    /**
     * 最大请求时间，超过该时间则视为超时（单位毫秒）
     */
    long maxRespTime() default 2000L;

    /**
     * 统计的最大请求数量
     */
    int maxReqSize() default 500;

    /**
     * 滑动单位
     */
    int slideUnit() default Integer.MAX_VALUE;

    /**
     * 允许的最大失败率，失败率超过该值将会被熔断
     */
    double maxFailRatio() default 0.2d;

    /**
     * 允许的最大超时率，超时率超过该值将会被熔断
     */
    double maxTimeoutRatio() default 0.7d;

    /**
     * 熔断时间（单位秒）
     */
    int fuseTime() default 5;

    /**
     * 非正常返回的异常类型
     */
    Class<? extends Throwable>[] notNormalExceptionTypes() default {ActivelyThrownException.class};

    /**
     * ID生成器，用于维度控制
     */
    Class<? extends IdGenerator> idGenerator() default IdGenerator.class;

}
