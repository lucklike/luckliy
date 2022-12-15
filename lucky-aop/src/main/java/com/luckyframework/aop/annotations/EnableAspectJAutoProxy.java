package com.luckyframework.aop.annotations;

import com.luckyframework.annotations.Import;
import com.luckyframework.aop.AspectJAdvisorBatchProductionPlant;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAdvisorBatchProductionPlant.class)
public @interface EnableAspectJAutoProxy {
}
