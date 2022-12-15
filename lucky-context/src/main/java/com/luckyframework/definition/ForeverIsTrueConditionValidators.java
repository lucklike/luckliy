package com.luckyframework.definition;

import com.luckyframework.annotations.ConditionContext;

public class ForeverIsTrueConditionValidators implements ConditionValidators {

    @Override
    public boolean matches(ConditionContext conditionContext) {
        return true;
    }
}
