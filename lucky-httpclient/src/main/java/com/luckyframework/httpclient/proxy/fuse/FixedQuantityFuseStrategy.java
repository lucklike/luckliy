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
@FuseMeta(fuse = @ObjectGenerate(clazz = LengthWindowFailureRateStatisticsFuseProtector.class, scope = Scope.CLASS))
public @interface FixedQuantityFuseStrategy {

    /**
     * 最大请求时间，超过该时间则视为超时
     */
    long maxRespTime() default 2000L;

    /**
     * 统计的最大请求数量
     */
    int maxReqSize() default 500;

    /**
     * 允许的最大失败率，失败率超过该值将会被熔断
     */
    double maxFailRatio() default 0.9;

    /**
     * 熔断时间（单位秒）
     */
    int fuseTimeSeconds() default 5;

    /**
     * 非正常返回的异常类型
     */
    Class<? extends Throwable>[] notNormalExceptionTypes() default {ActivelyThrownException.class};

    /**
     * ID生成器
     */
    Class<? extends IdGenerator> idGenerator() default IdGenerator.class;

}
