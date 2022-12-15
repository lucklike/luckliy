package com.luckyframework.annotations;


import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

import java.util.function.Predicate;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/29 0029 9:43
 */
public interface ImportSelector {

    /**
     * Select and return the names of which class(es) should be imported based on
     * the {@link AnnotationMetadata} of the importing @{@link Configuration} class.
     * @return the class names, or an empty array if none
     */
    String[] selectImports(AnnotationMetadata importingClassMetadata);

    /**
     * Return a predicate for excluding classes from the import candidates, to be
     * transitively applied to all classes found through this selector's imports.
     * <p>If this predicate returns {@code true} for a given fully-qualified
     * class name, said class will not be considered as an imported configuration
     * class, bypassing class file loading as well as metadata introspection.
     * @return the filter predicate for fully-qualified candidate class names
     * of transitively imported configuration classes, or {@code null} if none
     * @since 5.2.4
     */
    @Nullable
    default Predicate<String> getExclusionFilter() {
        return null;
    }

}
