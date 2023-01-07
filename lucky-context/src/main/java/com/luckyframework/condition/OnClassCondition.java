package com.luckyframework.condition;

import com.luckyframework.annotations.Condition;
import com.luckyframework.annotations.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class OnClassCondition implements Condition {

    private final static String ANNOTATION_STR = ConditionalOnClass.class.getName();

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            ClassLoader classLoader = context.getClassLoader();
            assert classLoader != null : "classLoader is null";
            Map<String, Object> attributes = metadata.getAnnotationAttributes(ANNOTATION_STR, true);
            String[] names = (String[]) attributes.get("name");
            String[] classNames = (String[]) attributes.get("value");
            for (String name : names) {
                classLoader.loadClass(name);
            }
            for (String className : classNames) {
                classLoader.loadClass(className);
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
