package com.luckyframework.annotations;


import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 条件筛选接口，只有当满足条件的组件才会被加载到容器中
 * @see Conditional
 * @author fk
 * @version 1.0
 * @date 2021/3/26 0026 14:33
 */
@FunctionalInterface
public interface Condition {

    boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

    static Condition alwaysSatisfiedCondition(){
        return ((context, metadata) -> true);
    }

}
