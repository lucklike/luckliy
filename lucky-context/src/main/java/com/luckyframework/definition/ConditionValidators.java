package com.luckyframework.definition;

import com.luckyframework.annotations.ConditionContext;

/**
 * 条件校验器接口
 */
public interface ConditionValidators {

    boolean matches(ConditionContext conditionContext);

}
