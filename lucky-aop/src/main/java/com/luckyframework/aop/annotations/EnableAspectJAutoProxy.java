package com.luckyframework.aop.annotations;

import com.luckyframework.annotations.Import;
import com.luckyframework.aop.AspectJAdvisorBatchProductionPlant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAdvisorBatchProductionPlant.class)
public @interface EnableAspectJAutoProxy {
}
