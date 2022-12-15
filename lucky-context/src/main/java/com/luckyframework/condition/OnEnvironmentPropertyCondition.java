package com.luckyframework.condition;

import com.luckyframework.annotations.Condition;
import com.luckyframework.annotations.ConditionContext;
import com.luckyframework.environment.LuckyStandardEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class OnEnvironmentPropertyCondition implements Condition {

    private final static String ANNOTATION_STR = ConditionalOnEnvironmentProperty.class.getName();

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        LuckyStandardEnvironment environment = (LuckyStandardEnvironment) context.getEnvironment();
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ANNOTATION_STR);
        String propertyName = (String) attributes.get("propertyName");
        String configValue = environment.getProperty(propertyName, String.class);
        return "true".equalsIgnoreCase(configValue);
    }
}
