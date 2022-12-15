package com.luckyframework.condition;

import com.luckyframework.annotations.Condition;
import com.luckyframework.annotations.ConditionContext;
import com.luckyframework.bean.aware.ClassLoaderAware;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class OnClassCondition implements Condition, ClassLoaderAware {

    private final static String ANNOTATION_STR = ConditionalOnClass.class.getName();
    private ClassLoader classLoader;

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            Map<String, Object> attributes = metadata.getAnnotationAttributes(ANNOTATION_STR);
            String[] classNames = (String[]) attributes.get("name");
            for (String className : classNames) {
                Class.forName(className,false,classLoader);
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
