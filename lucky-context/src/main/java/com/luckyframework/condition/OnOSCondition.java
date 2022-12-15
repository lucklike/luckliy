package com.luckyframework.condition;

import com.luckyframework.annotations.Condition;
import com.luckyframework.annotations.ConditionContext;
import com.luckyframework.environment.LuckyStandardEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/26 20:06
 */
public class OnOSCondition implements Condition {

    private final String ANNOTATION_STR = ConditionalOnOS.class.getName();

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        LuckyStandardEnvironment environment = (LuckyStandardEnvironment) context.getEnvironment();
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ANNOTATION_STR);
        Object configOsName = attributes.get("value");
        String osName = environment.getProperty("os.name");
        assert osName != null;
        return osName.toUpperCase().contains(String.valueOf(configOsName).toUpperCase());
    }
}
