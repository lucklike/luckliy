package com.luckyframework.reflect;

import com.luckyframework.exception.LuckyReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 注解工具类，用于处理于注解相关的事情
 *
 * @author fk7075
 * @version 1.0
 * @date 2020/10/26 16:33
 */
@SuppressWarnings("all")
public abstract class AnnotationUtils extends AnnotatedElementUtils {

    private static final Logger log = LoggerFactory.getLogger(AnnotationUtils.class);

    /**
     * 判断注解元素是否被制定注解标注
     *
     * @param annotatedElement 注解元素
     * @param annotationClass  注解Class
     * @return true/false
     */
    public static boolean isExist(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return annotatedElement.isAnnotationPresent(annotationClass);
    }

    /**
     * 判断一个注解元素是否被注解数组中的某一个标注
     *
     * @param annotatedElement  注解元素
     * @param annotationClasses 注解Class数组
     * @return true/false
     */
    public static boolean isExistOrByArray(AnnotatedElement annotatedElement, Class<? extends Annotation>[] annotationClasses) {
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            if (isExist(annotatedElement, annotationClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取注解元素上某个制定注解的实例
     *
     * @param annotatedElement 注解元素
     * @param annotationClass  注解Class
     * @param <T>              注解泛型
     * @return 注解实例
     */
    public static <T extends Annotation> T get(AnnotatedElement annotatedElement, Class<T> annotationClass) {
        if (isExist(annotatedElement, annotationClass)) {
            return annotatedElement.getAnnotation(annotationClass);
        }
        throw new AnnotationParsingException("An exception occurs when getting the annotation instance, the annotation element '" + annotatedElement + "' is not annotated by '" + annotationClass + "' annotation.");
    }

    /**
     * 动态的设置注解某个属性的值
     *
     * @param annoation               注解实例
     * @param annotationAttributeName 需要修改的属性的属性名
     * @param newValue                要设置的值
     * @param <A>                     注解泛型
     */
    public static <A extends Annotation> void setValue(A annoation, String annotationAttributeName, Object newValue) {
        try {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(annoation);
            Field memberValues = FieldUtils.getDeclaredField(invocationHandler.getClass(), "memberValues");
            memberValues.setAccessible(true);
            Map map = (Map) memberValues.get(invocationHandler);
            map.put(annotationAttributeName, newValue);
        } catch (IllegalAccessException e) {
            throw new LuckyReflectionException(e);
        }
    }

    /**
     * 动态的设置注解元素上某个注解的属性值
     *
     * @param annotatedElement        注解原属
     * @param annotationClass         注解类型
     * @param annotationAttributeName 注解属性名
     * @param newValue                要设置的属性值
     * @param <A>                     注解泛型
     */
    public static <A extends Annotation> void setValue(AnnotatedElement annotatedElement, Class<A> annotationClass, String annotationAttributeName, Object newValue) {
        setValue(findMergedAnnotation(annotatedElement, annotationClass), annotationAttributeName, newValue);
    }

    /**
     * 动态的设置注解元素上某个注解的属性值
     *
     * @param annotatedElement        注解原属
     * @param annotationClassName     注解类型的全类名
     * @param annotationAttributeName 注解属性名
     * @param newValue                要设置的属性值
     * @param <A>                     注解泛型
     */
    public static void setValue(AnnotatedElement annotatedElement, String annotationClassName, String annotationAttributeName, Object newValue) {
        setValue(findMergedAnnotation(annotatedElement, (Class<Annotation>) ClassUtils.forName(annotationClassName, ClassUtils.getDefaultClassLoader())), annotationAttributeName, newValue);
    }

    /**
     * 获取注解中某个制定属性的值
     *
     * @param annotation              注解实例
     * @param annotationAttributeName 需要获取的注解的属性名
     * @param <A>                     注解泛型
     * @return 注解属性值
     */
    public static <A extends Annotation> Object getValue(A annotation, String annotationAttributeName) {
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
        Map<?, ?> map = (Map<?, ?>) FieldUtils.getValue(invocationHandler, "memberValues");
        return map.get(annotationAttributeName);
    }

    /**
     * 获取注解元素中制定注解指定属性的值
     *
     * @param annotatedElement        注解元素
     * @param annotationClass         待获取的注解的类型
     * @param annotationAttributeName 需要获取的注解属性的属性名
     * @param <A>                     注解泛型
     * @return 注解元素中制定注解指定属性的值
     */
    public static <A extends Annotation> Object getValue(AnnotatedElement annotatedElement, Class<A> annotationClass, String annotationAttributeName) {
        return getMergedAnnotationAttributes(annotatedElement, annotationClass).get(annotationAttributeName);
    }

    /**
     * 获取注解元素中制定注解指定属性的值
     *
     * @param annotatedElement        注解元素
     * @param annotationClassName     待获取的注解的类型的全类名
     * @param annotationAttributeName 需要获取的注解属性的属性名
     * @return 注解元素中制定注解指定属性的值
     */
    public static Object getValue(AnnotatedElement annotatedElement, String annotationClassName, String annotationAttributeName) {
        return getMergedAnnotationAttributes(annotatedElement, annotationClassName).get(annotationAttributeName);
    }

    /**
     * 得到注解元素中包含的所有目标注解（包含组合注解中的目标注解）
     *
     * @param annotatedElement 注解元素
     * @param annoationClass   目标注解的Class
     * @param <A>              目标注解的类型
     * @return 注解元素中包含的所有目标注解（包含组合注解中的目标注解）
     */
    public static <A extends Annotation> List<A> strengthenGet(AnnotatedElement annotatedElement, Class<A> annoationClass) {
        List<A> resultAnnooations = new ArrayList<>();
        List<Annotation> beyondMetaAnnotations = filterMetaAnnotation(annotatedElement.getAnnotations());
        for (Annotation annotation : beyondMetaAnnotations) {
            if (annotation.annotationType() == annoationClass) {
                resultAnnooations.add((A) annotation);
            }
            resultAnnooations.addAll(strengthenGet(annotation.annotationType(), annoationClass));
        }
        return resultAnnooations;
    }

    /**
     * 加强版的类注解标注检查，针对组合注解的检查
     *
     * @param annotatedElement 注解原属
     * @param annoationClass   待判断的注解类型
     * @param <A>              注解的类型
     * @return true/false
     */
    public static <A extends Annotation> boolean strengthenIsExist(AnnotatedElement annotatedElement, Class<A> annoationClass) {
        if (isExist(annotatedElement, annoationClass)) {
            return true;
        }
        List<Annotation> annotations = filterMetaAnnotation(annotatedElement.getAnnotations());
        for (Annotation annotation : annotations) {
            if (strengthenIsExist(annotation.annotationType(), annoationClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤掉注解数组中的元注解
     *
     * @param annotations 注解数组
     * @return 过滤调元注解后的注解集合
     */
    public static List<Annotation> filterMetaAnnotation(Annotation[] annotations) {
        return Stream.of(annotations).filter((a) -> {
            boolean i = a instanceof Inherited;
            boolean r = a instanceof Retention;
            boolean d = a instanceof Documented;
            boolean t = a instanceof Target;
            boolean r1 = a instanceof Repeatable;
            boolean o = a instanceof Override;
            boolean s = a instanceof SuppressWarnings;
            return !(i | r | d | t | r1 | o | s);
        }).collect(Collectors.toList());
    }

    /**
     * 获取一个Spring组合注解代理对象的实际注解对象对应的MergedAnnotation
     *
     * @param annotation 带解析的注解实例
     * @return Spring组合注解代理对象的实际注解对象对应的MergedAnnotation
     */
    public static MergedAnnotation<?> getSpringRootMergedAnnotation(Annotation annotation) {
        InvocationHandler handler = Proxy.getInvocationHandler(annotation);
        MergedAnnotation<?> mergedAnnotation = (MergedAnnotation<?>) FieldUtils.getValue(handler, "annotation");
        return mergedAnnotation.getRoot();
    }

    /**
     * 获取一个Spring组合注解代理对象的实际注解对象
     *
     * @param annotation 带解析的注解实例
     * @return Spring组合注解代理对象的实际注解对象
     */
    public static Annotation getgetSpringRootAnnotation(Annotation annotation) {
        return getSpringRootMergedAnnotation(annotation).synthesize();
    }

    public static Set<Annotation> getAnnotationsByContain(AnnotatedElement annotatedElement, Class<? extends Annotation> sourceAnnClass) {
        List<Annotation> annotationList = filterMetaAnnotation(annotatedElement.getAnnotations());
        return annotationList
                .stream()
                .filter(a -> a.annotationType() == sourceAnnClass || isAnnotated(a.annotationType(), sourceAnnClass))
                .collect(Collectors.toSet());
    }

}
