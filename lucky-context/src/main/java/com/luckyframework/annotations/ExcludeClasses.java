package com.luckyframework.annotations;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/24 下午11:17
 */
public interface ExcludeClasses extends ExcludeClassNames {

    @NonNull
    Class<?>[] excludeClasses(List<AnnotationMetadata> scannerElements,AnnotationMetadata excludeClassMetadata);

    @Override
    default String[] excludeClassNames(List<AnnotationMetadata> scannerElements,AnnotationMetadata excludeClassMetadata) {
        Class<?>[] classes = excludeClasses(scannerElements,excludeClassMetadata);
        String[] classNames = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            classNames[i] = classes[i].getName();
        }
        return classNames;
    }
}
