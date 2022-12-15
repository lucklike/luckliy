package com.luckyframework.annotations;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/24 下午11:17
 */
public interface ExcludeClassNames {

    @NonNull
    String[] excludeClassNames(List<AnnotationMetadata> scannerElements,AnnotationMetadata excludeClassMetadata);
}
