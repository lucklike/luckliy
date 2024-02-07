package com.luckyframework.reflect;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(annoation);
        if (invocationHandler instanceof CombinationAnnotationInvocationHandler) {
            ((CombinationAnnotationInvocationHandler) invocationHandler).setValue(annotationAttributeName, newValue);
        } else if (invocationHandler instanceof ExtendAnnotationInvocationHandler) {
            ((ExtendAnnotationInvocationHandler) invocationHandler).setValue(annotationAttributeName, newValue);
        } else {
            Map map = (Map) FieldUtils.getValue(invocationHandler, "memberValues");
            map.put(annotationAttributeName, newValue);
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
        if (invocationHandler instanceof CombinationAnnotationInvocationHandler) {
            return ((CombinationAnnotationInvocationHandler) invocationHandler).getAnnotationAttribute(annotationAttributeName);
        }
        if (invocationHandler instanceof ExtendAnnotationInvocationHandler) {
            return ((ExtendAnnotationInvocationHandler) invocationHandler).getAnnotationAttribute(annotationAttributeName);
        }
        if (invocationHandler instanceof AnnotationTypeConversionInvocationHandler) {
            return ((AnnotationTypeConversionInvocationHandler) invocationHandler).getAnnotationAttribute(annotationAttributeName);
        }
        try {
            // 使用JDK注解代理的方式处理
            Map<?, ?> map = (Map<?, ?>) FieldUtils.getValue(invocationHandler, "memberValues");
            return map.get(annotationAttributeName);
        } catch (LuckyReflectionException e) {
            // 出现异常时使用Spring注解代理的方式处理
            MergedAnnotation<?> mergedAnnotation = (MergedAnnotation<?>) FieldUtils.getValue(invocationHandler, "annotation");
            if (mergedAnnotation.asMap().containsKey(annotationAttributeName)) {
                return mergedAnnotation.asMap().get(annotationAttributeName);
            }
            MergedAnnotation<?> annotationRoot = mergedAnnotation.getRoot();
            if (annotationRoot.asMap().containsKey(annotationAttributeName)) {
                return annotationRoot.asMap().get(annotationAttributeName);
            }
            throw new LuckyReflectionException("The attribute named '{}' is not found in the @'{}' annotation", annotationAttributeName, annotation.annotationType().getName());
        }
    }

    /**
     * 获取注解中某个属性的值，并将其转化为指定的类型
     *
     * @param annotation              注解实例
     * @param annotationAttributeName 需要获取的注解的属性名
     * @param type                    返回值类型
     * @param <A>                     注解类型泛型
     * @param <T>                     返回值类型泛型
     * @return 指定类型的注解属性值
     */
    public static <A extends Annotation, T> T getValue(A annotation, String annotationAttributeName, Class<T> type) {
        Object value = getValue(annotation, annotationAttributeName);
        return ConversionUtils.conversion(value, type);
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
    public static List<Annotation> filterMetaAnnotation(Collection<Annotation> collections) {
        return filterMetaAnnotation(collections.toArray(new Annotation[0]));
    }

    /**
     * 过滤掉注解数组中的元注解
     *
     * @param annotations 注解数组
     * @return 过滤调元注解后的注解集合
     */
    public static List<Annotation> filterMetaAnnotation(Annotation[] annotations) {
        return Stream.of(annotations).filter(a -> !a.annotationType().getName().startsWith("java.lang.")).collect(Collectors.toList());
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
    public static Annotation getSpringRootAnnotation(Annotation annotation) {
        return getSpringRootMergedAnnotation(annotation).synthesize();
    }

    public static <A extends Annotation> A toExtendAnnotation(@NonNull A annotation) {
        ExtendAnnotationInvocationHandler invocationHandler = new ExtendAnnotationInvocationHandler(annotation);
        return (A) invocationHandler.getProxyAnnotation();
    }

    public static <A extends Annotation> A toAnnotation(@NonNull Annotation sourceAnnotation, Class<A> targetAnnotationType) {
        AnnotationTypeConversionInvocationHandler invocationHandler = new AnnotationTypeConversionInvocationHandler(sourceAnnotation, targetAnnotationType);
        return (A) invocationHandler.getProxyAnnotation();
    }

    public static Set<Annotation> getContainCombinationAnnotationsIgnoreSource(AnnotatedElement annotatedElement, Class<? extends Annotation> sourceAnnClass) {
        return getContainCombinationAnnotations(annotatedElement, sourceAnnClass, true);
    }

    public static Set<Annotation> getContainCombinationAnnotations(AnnotatedElement annotatedElement, Class<? extends Annotation> sourceAnnClass) {
        return getContainCombinationAnnotations(annotatedElement, sourceAnnClass, false);
    }

    /**
     * <pre>
     *     获取注解元素上面满足如下条件的注解：
     *     1.是否收集sourceAnnClass注解本身取决于ignoreSourceAnn
     *     2.被sourceAnnClass注解标注的注解
     *     3.sourceAnnClass的重复注解(@Repeatable)
     * </pre>
     *
     * @param annotatedElement 注解元素
     * @param sourceAnnClass   待校验的注解
     * @return 满足要求的所有注解实例
     */
    public static Set<Annotation> getContainCombinationAnnotations(AnnotatedElement annotatedElement, Class<? extends Annotation> sourceAnnClass, boolean ignoreSourceAnn) {
        Repeatable repeatableAnn = AnnotationUtils.findMergedAnnotation(sourceAnnClass, Repeatable.class);
        List<Annotation> annotationList = filterMetaAnnotation(getCombinationAnnotations(annotatedElement));

        // 没有被@Repeatable注解标记的情况
        if (repeatableAnn == null) {
            Set<Annotation> resultSet = new HashSet<>();
            for (Annotation annotation : annotationList) {
                Set<Annotation> annotationsAndCombine = combineAndGetAnnotationSuperiorAnnotations(annotation);
                for (Annotation ann : annotationsAndCombine) {
                    Class<? extends Annotation> annType = ann.annotationType();
                    if ((annType == sourceAnnClass && !ignoreSourceAnn) || isAnnotated(annType, sourceAnnClass)) {
                        resultSet.add(ann);
                    } else if (!isCombinedAnnotationInstance(ann)) {
                        resultSet.addAll(getContainCombinationAnnotations(annType, sourceAnnClass, ignoreSourceAnn));
                    }
                }
            }
            return resultSet;
        }

        // 被@Repeatable注解标记的情况，此时需要考虑组合注解的情况
        Class<? extends Annotation> repeatableClass = repeatableAnn.value();
        Set<Annotation> resultSet = new HashSet<>();
        for (Annotation annotation : annotationList) {
            Set<Annotation> annotationsAndCombine = combineAndGetAnnotationSuperiorAnnotations(annotation);
            for (Annotation ann : annotationsAndCombine) {
                Class<? extends Annotation> annType = ann.annotationType();
                if ((annType == sourceAnnClass && !ignoreSourceAnn) || isAnnotated(annType, sourceAnnClass)) {
                    resultSet.add(ann);
                } else if (annType == repeatableClass) {
                    Annotation[] valueArray = (Annotation[]) AnnotationUtils.getValue(annotation, "value");
                    resultSet.addAll(Arrays.asList(valueArray));
                } else if (!isCombinedAnnotationInstance(ann)) {
                    resultSet.addAll(getContainCombinationAnnotations(annType, sourceAnnClass, ignoreSourceAnn));
                }
            }
        }
        return resultSet;
    }

    /**
     * 将多个注解组合成一个组合注解，元素注解实例不可为null，否则会抛出异常
     *
     * @param annType     合成之后的组合注解类型
     * @param annotations 用于合成组合注解的注解
     * @param <T>         组合注解类型
     * @return 多个注解组合成一个组合注解
     */
    public static <T extends Annotation> T createCombinationAnnotation(Class<T> annType, @NonNull Annotation... annotations) {
        CombinationAnnotationInvocationHandler invocationHandler = new CombinationAnnotationInvocationHandler(annType);
        invocationHandler.addAnnotations(annotations);
        return (T) invocationHandler.getProxyAnnotation();
    }

    /**
     * 将多个注解组合成一个组合注解，元素注解实例不可为null，否则会抛出异常
     *
     * @param annType     合成之后的组合注解类型
     * @param annotations 用于合成组合注解的注解
     * @param <T>         组合注解类型
     * @return 多个注解组合成一个组合注解
     */
    public static <T extends Annotation> T createCombinationAnnotationIgnoreNullELement(Class<T> annType, @NonNull Annotation... annotations) {
        CombinationAnnotationInvocationHandler invocationHandler = new CombinationAnnotationInvocationHandler(annType);
        invocationHandler.addAnnotationIgnoreNullELement(annotations);
        return (T) invocationHandler.getProxyAnnotation();
    }

    /**
     * 将相同的注解进行组合，形成新的注解实例
     * <pre>
     *     1. 如果注解元素是{@link Class}，直接使用{@link #findMergedAnnotation(AnnotatedElement, Class)}获取注解之后进行返回。
     *     2. 如果注解元素是{@link java.lang.reflect.Field Field}或{@link Method}，则尝试获取{@link java.lang.reflect.Field Field}或{@link Method}上的注解之后，在去获取
     *        {@link Class}上相同的注解，最后将这两个注解进行组合知乎返回。
     *     3. 如果注解元素是{@link Parameter}，则尝试获取{@link Parameter}、{@link Method}以及{@link Class}上的注解，然后将这三个注解组合起来之后再进行返回。
     * </pre>
     *
     * @param annotatedElement
     * @param annotationType
     * @param <T>
     * @return
     */
    @Nullable
    public static <T extends Annotation> T sameAnnotationCombined(AnnotatedElement annotatedElement, Class<T> annotationType) {

        if (annotatedElement instanceof Class) {
            return findMergedAnnotation(annotatedElement, annotationType);
        }

        if (annotatedElement instanceof Member) {
            Member member = (Member) annotatedElement;
            T mergedAnnotation = findMergedAnnotation(annotatedElement, annotationType);
            T classAnnotation = findMergedAnnotation(member.getDeclaringClass(), annotationType);
            if (classAnnotation == null) {
                return mergedAnnotation;
            }
            if (mergedAnnotation == null) {
                return classAnnotation;
            }
            return createCombinationAnnotation(annotationType, mergedAnnotation, classAnnotation);
        }

        if (annotatedElement instanceof Parameter) {
            Parameter parameter = (Parameter) annotatedElement;
            Executable declaringExecutable = parameter.getDeclaringExecutable();
            Class<?> declaringClass = declaringExecutable.getDeclaringClass();

            T parameterAnnotation = findMergedAnnotation(parameter, annotationType);
            T declaringExecutableAnnotation = findMergedAnnotation(declaringExecutable, annotationType);
            T declaringClassAnnotation = findMergedAnnotation(declaringClass, annotationType);

            if (parameterAnnotation == null && declaringExecutableAnnotation == null) {
                return declaringClassAnnotation;
            }
            if (parameterAnnotation == null && declaringClassAnnotation == null) {
                return declaringExecutableAnnotation;
            }
            if (declaringExecutableAnnotation == null && declaringClassAnnotation == null) {
                return parameterAnnotation;
            }
            return createCombinationAnnotationIgnoreNullELement(annotationType, parameterAnnotation, declaringExecutableAnnotation, declaringClassAnnotation);
        }
        throw new LuckyReflectionException("Unparsed Annotated Element type: {}", annotationType);
    }

    /**
     * 判断某个注解是否为组合注解(是否被{@link Combination}注解标注)
     *
     * @param annotationType 待判断的注解类型
     * @return 该是否为组合注解
     */
    public static boolean isCombinedAnnotation(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(Combination.class);
    }

    /**
     * 判断某个注解实例是否为组合注解实例(是否为{@link CombinationAnnotationInvocationHandler}代理生成的注解实例)
     *
     * @param annotationType 待判断的注解实例
     * @return 该注解实例是否为组合注解
     */
    public static boolean isCombinedAnnotationInstance(Annotation annotation) {
        return Proxy.getInvocationHandler(annotation) instanceof CombinationAnnotationInvocationHandler;
    }

    /**
     * 获取注解元素上的组合注解
     * <pre>
     *  1.如果注解元素上不存在指定的注解时返回null。
     *  2.如果指定的注解为非组合注解时返回该注解实例本身
     *  3.如果指定的注解为组合注解时，获取该注解实例的同时还需要获取该注解上被{@link Combination}指定的注解实例，然后利用这些注解实例共同组合形成一个组合注解返回
     * </pre>
     *
     * @param annotatedElement 组合注解
     * @param annotationType   组合注解的类型Clas
     * @param <A>              组合注解的类型
     * @return 注解元素上的组合注解
     */
    public static <A extends Annotation> A getCombinationAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        return getCombinationAnnotation(findMergedAnnotation(annotatedElement, annotationType));
    }

    public static List<Annotation> getCombinationAnnotations(AnnotatedElement annotatedElement) {
        Annotation[] markedAnns = annotatedElement.getAnnotations();
        List<Annotation> markedMeagerAnns = new ArrayList<>(markedAnns.length);

        for (Annotation markedAnn : markedAnns) {
            markedMeagerAnns.add(getCombinationAnnotation(findMergedAnnotation(annotatedElement, markedAnn.annotationType())));
        }

        return markedMeagerAnns;
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getCombinationAnnotation(A sourceAnn) {
        // 原注解为null或原注解不是合并注解时，直接返回原注解
        if (sourceAnn == null || !isCombinedAnnotation(sourceAnn.annotationType())) {
            return sourceAnn;
        }
        // 是合并注解的情况
        Class<? extends Annotation> annotationType = sourceAnn.annotationType();
        Combination combinationAnn = annotationType.getAnnotation(Combination.class);
        Class<? extends Annotation>[] combinationClasses = combinationAnn.value();
        List<Annotation> sourceLabelAnnList = filterMetaAnnotation(getCombinationAnnotations(sourceAnn.annotationType()));
        List<Annotation> combinationAnnList = new ArrayList<>(combinationClasses.length);
        combinationAnnList.add(sourceAnn);
        out:
        for (Class<? extends Annotation> combinationClass : combinationClasses) {
            for (Annotation ann : sourceLabelAnnList) {
                if (ann.annotationType() == combinationClass) {
                    combinationAnnList.add(ann);
                    continue out;
                }
            }
        }
        return (A) createCombinationAnnotation(annotationType, combinationAnnList.toArray(new Annotation[0]));
    }

    /**
     * 组合并获取注解上的所有注解
     * <pre>
     *  1.该注解实例为null或者类型不是组合注解类型时，直接返回该注解上标注的所有注解以及该注解本身所组成的集合
     *  2.该注解实例类型为组合注解类型时，返回{@link Combination}注解中指定的的注解实例与该注解实例组成的组合注解和其他注解实例所组成的集合
     * </pre>
     *
     * @param annotation 待操作的注解
     * @return 组合并获取注解上的所有注解
     */
    public static Set<Annotation> combineAndGetAnnotationSuperiorAnnotations(Annotation annotation) {
        if (annotation == null || !isCombinedAnnotation(annotation.annotationType())) {
            List<Annotation> annotations = filterMetaAnnotation(getCombinationAnnotations(annotation.annotationType()));
            annotations.add(annotation);
            return new HashSet<>(annotations);
        }
        Class<? extends Annotation> annotationType = annotation.annotationType();
        Combination combinationAnn = annotationType.getAnnotation(Combination.class);
        Class<? extends Annotation>[] combinationClasses = combinationAnn.value();

        List<Annotation> allAnnotations = filterMetaAnnotation(getCombinationAnnotations(annotationType));
        List<Annotation> combinationAnnList = new ArrayList<>();
        List<Annotation> otherAnnList = new ArrayList<>();
        combinationAnnList.add(annotation);
        out:
        for (Class<? extends Annotation> combinationClass : combinationClasses) {
            for (Annotation ann : allAnnotations) {
                if (ann.annotationType() == combinationClass) {
                    combinationAnnList.add(ann);
                    continue out;
                }
            }
        }

        Annotation combinationAnnotation = createCombinationAnnotation(annotationType, combinationAnnList.toArray(new Annotation[0]));
        Set<Annotation> combinationAnnotationSet = new HashSet<>();
        combinationAnnotationSet.add(combinationAnnotation);
        for (Annotation ann : allAnnotations) {
            if (!combinationAnnList.contains(ann)) {
                combinationAnnotationSet.add(getCombinationAnnotation(ann));
            }
        }
        return combinationAnnotationSet;
    }

    public static Object getDefaultValue(Annotation annotation, String attributeName) {
        InvocationHandler handler = Proxy.getInvocationHandler(annotation);
        if (handler instanceof CombinationAnnotationInvocationHandler) {
            return ((CombinationAnnotationInvocationHandler) handler).getDefaultValue(attributeName);
        }
        if (handler instanceof ExtendAnnotationInvocationHandler) {
            return ((ExtendAnnotationInvocationHandler) handler).getDefaultValue(attributeName);
        }
        try {
            MergedAnnotation<?> springRootMergedAnnotation = getSpringRootMergedAnnotation(annotation);
            return springRootMergedAnnotation.getDefaultValue(attributeName).get();
        } catch (Exception e) {
            try {
                return MethodUtils.getDeclaredMethod(annotation.annotationType(), attributeName).getDefaultValue();
            } catch (Exception e2) {
                // ignore
            }

        }
        return null;
    }

    public static ResolvableType getAttributeType(Annotation annotation, String attributeName) {
        Method attributeMethod = getAttributeMethod(annotation, attributeName);
        return attributeMethod == null ? null : ResolvableType.forMethodReturnType(attributeMethod);
    }

    public static Method getAttributeMethod(Annotation annotation, String attributeName) {
        InvocationHandler handler = Proxy.getInvocationHandler(annotation);
        if (handler instanceof CombinationAnnotationInvocationHandler) {
            return ((CombinationAnnotationInvocationHandler) handler).getAttributeMethod(attributeName);
        }
        if (handler instanceof ExtendAnnotationInvocationHandler) {
            return ((ExtendAnnotationInvocationHandler) handler).getAttributeMethod(attributeName);
        }
        try {
            return MethodUtils.getDeclaredMethod(annotation.annotationType(), attributeName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 组合注解拦截器
     */
    static final class CombinationAnnotationInvocationHandler implements InvocationHandler {

        /**
         * 注解类型
         */
        private final Class<? extends Annotation> annotationType;
        /**
         * 源注解集合，当获取组合注解属性值时会依次遍历该集合中的注解实例并寻找同名的属性
         */
        private final List<Annotation> annotationList = new ArrayList<>();
        /**
         * 组合后注解的所包含的方法
         */
        private final Set<Method> annotationMethods;

        /**
         * 属性名和对应方法组成的Map
         */
        private final Map<String, Method> attributeMethodMap = new ConcurrentHashMap<>(8);
        /**
         * 默认值缓存
         */
        private final Map<String, Object> defaultValueMap = new ConcurrentHashMap<>(8);
        /**
         * 当前注解属性值缓存
         */
        private final Map<String, Object> currentAnnotationAttributeValueMap = new ConcurrentHashMap<>(8);
        /**
         * 其他注解属性值缓存
         */
        private final Map<String, Object> otherAnnotationAttributeValueMap = new ConcurrentHashMap<>(8);

        /**
         * toString()方法返回该值
         */
        private String string;
        /**
         * hashCode
         */
        private Integer hashCode;

        CombinationAnnotationInvocationHandler(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
            this.annotationMethods = ContainerUtils.arrayToSet(annotationType.getDeclaredMethods());
            this.attributeMethodMap.putAll(annotationMethods.stream().collect(Collectors.toMap(Method::getName, m -> m)));
        }

        public void addAnnotations(boolean ignoreNullElement, Annotation... annotations) {
            for (int i = 0; i < annotations.length; i++) {
                if (annotations[i] == null) {
                    if (!ignoreNullElement) {
                        throw new IllegalArgumentException("The annotation element used to generate the combined annotation cannot be null. index is [" + i + "]");
                    }
                } else {
                    this.annotationList.add(annotations[i]);
                }
            }
        }

        public void addAnnotations(boolean ignoreNullElement, Collection<Annotation> annotations) {
            addAnnotations(ignoreNullElement, annotations.toArray(new Annotation[0]));
        }

        public void addAnnotations(Annotation... annotations) {
            addAnnotations(false, annotations);
        }

        public void addAnnotationIgnoreNullELement(Annotation... annotations) {
            addAnnotations(true, annotations);
        }

        public void addAnnotations(Collection<Annotation> annotations) {
            addAnnotations(false, annotations);
        }

        public void addAnnotationIgnoreNullELement(Collection<Annotation> annotations) {
            addAnnotations(true, annotations);
        }


        public Annotation getProxyAnnotation() {
            return (Annotation) ProxyFactory.getJdkProxyObject(annotationType, this);
        }

        public Object getAnnotationAttribute(String attributeName) {
            if (attributeMethodMap.containsKey(attributeName)) {
                return currentAnnotationAttributeValueMap.computeIfAbsent(attributeName, this::doGetAnnotationAttribute);
            }
            return otherAnnotationAttributeValueMap.computeIfAbsent(attributeName, this::doGetAnnotationAttribute);
        }

        public Object getDefaultValue(String attributeName) {
            return this.defaultValueMap.computeIfAbsent(attributeName, this::doGetDefaultValue);
        }

        public Method getAttributeMethod(String attributeName) {
            for (Annotation annotation : annotationList) {
                Method returnType = AnnotationUtils.getAttributeMethod(annotation, attributeName);
                if (returnType != null) {
                    return returnType;
                }
            }
            return null;
        }

        public void setValue(String attributeName, Object value) {
            Method method = this.attributeMethodMap.get(attributeName);
            if (method == null) {
                throw new LuckyReflectionException("The attribute named '{}' is not found in the @'{}' annotation", attributeName, this.annotationType.getName());
            }
            try {
                this.currentAnnotationAttributeValueMap.put(attributeName, ConversionUtils.conversion(value, ResolvableType.forMethodReturnType(method)));
                this.string = null;
            } catch (Exception e) {

                throw new LuckyReflectionException("The {} value {} cannot be assigned to the annotated @{} attribute '{}' of type {}",
                        value.getClass(), value, this.annotationType.getName(), attributeName, method.getReturnType().getName()
                );
            }
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (ReflectionUtils.isEqualsMethod(method)) {
                return annotationEquals(args[0]);
            }
            if (ReflectionUtils.isHashCodeMethod(method)) {
                return annotationHashCode();
            }
            if (ReflectionUtils.isToStringMethod(method)) {
                return annotationToString();
            }
            if (isAnnotationTypeMethod(method)) {
                return this.annotationType;
            }
            if (isAnnotationMethod(method)) {
                return getAnnotationAttribute(method.getName());
            }
            throw new AnnotationConfigurationException(String.format("Method [%s] is unsupported for synthesized annotation type [%s]", method, this.annotationType));
        }

        //--  private  --//

        private Object doGetAnnotationAttribute(String attributeName) {
            Object defaultValue = getDefaultValue(attributeName);
            RuntimeException ex = null;
            for (Annotation annotation : this.annotationList) {
                try {
                    Object currentValue = getValue(annotation, attributeName);
                    if (currentValue != null && !currentValue.equals(defaultValue)) {
                        return currentValue;
                    }
                } catch (LuckyReflectionException e) {
                    ex = e;
                }
            }
            if (defaultValue != null || ex == null) {
                return defaultValue;
            }
            throw ex;
        }

        private Object doGetDefaultValue(String attributeName) {
            Object defaultValue;
            for (Annotation annotation : this.annotationList) {
                defaultValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
                if (defaultValue != null) {
                    return defaultValue;
                }
            }
            return null;
        }

        private boolean annotationEquals(Object other) {
            if (this == other) {
                return true;
            }
            if (!this.annotationType.isInstance(other)) {
                return false;
            }
            return annotationToString().equals(other.toString());
        }

        private int annotationHashCode() {
            Integer hashCode = this.hashCode;
            if (hashCode == null) {
                hashCode = computeHashCode();
                this.hashCode = hashCode;
            }
            return hashCode;
        }

        private Integer computeHashCode() {
            int hashCode = 0;
            for (Method method : annotationMethods) {
                Object value = getAnnotationAttribute(method.getName());
                hashCode += (127 * method.getName().hashCode()) ^ getValueHashCode(value);
            }
            return hashCode;
        }

        public String annotationToString() {
            String string = this.string;
            if (string == null) {
                StringBuilder builder = new StringBuilder("@").append(StringUtils.getClassName(this.annotationType)).append('(');
                int i = 0;
                for (Method attribute : this.annotationMethods) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    String name = attribute.getName();
                    builder.append(name);
                    builder.append('=');
                    builder.append(StringUtils.toString(getAnnotationAttribute(name)));
                    i++;
                }
                builder.append(')');
                string = builder.toString();
                this.string = string;
            }
            return string;
        }

        private int getValueHashCode(Object value) {
            // Use Arrays.hashCode(...) since Spring's ObjectUtils doesn't comply
            // with the requirements specified in Annotation#hashCode().
            if (value instanceof boolean[]) {
                return Arrays.hashCode((boolean[]) value);
            }
            if (value instanceof byte[]) {
                return Arrays.hashCode((byte[]) value);
            }
            if (value instanceof char[]) {
                return Arrays.hashCode((char[]) value);
            }
            if (value instanceof double[]) {
                return Arrays.hashCode((double[]) value);
            }
            if (value instanceof float[]) {
                return Arrays.hashCode((float[]) value);
            }
            if (value instanceof int[]) {
                return Arrays.hashCode((int[]) value);
            }
            if (value instanceof long[]) {
                return Arrays.hashCode((long[]) value);
            }
            if (value instanceof short[]) {
                return Arrays.hashCode((short[]) value);
            }
            if (value instanceof Object[]) {
                return Arrays.hashCode((Object[]) value);
            }
            return value.hashCode();
        }

        private boolean isAnnotationTypeMethod(Method method) {
            return (method.getName().equals("annotationType") && method.getParameterCount() == 0);
        }

        private boolean isAnnotationMethod(Method method) {
            return this.annotationMethods.contains(method);
        }


    }

    /**
     * 扩展注解拦截器
     */
    static final class ExtendAnnotationInvocationHandler implements InvocationHandler {

        private final Annotation source;
        private final Set<Method> annotationMethods;

        /**
         * 当前注解属性值缓存
         */
        private final Map<String, Object> valueMap = new ConcurrentHashMap<>(8);

        /**
         * 属性名和对应方法组成的Map
         */
        private final Map<String, Method> attributeMethodMap = new ConcurrentHashMap<>(8);
        /**
         * 默认值缓存
         */
        private final Map<String, Object> defaultValueMap = new ConcurrentHashMap<>(8);

        /**
         * 扩展属性映射
         */
        private final Map<String, String> extendAttributeMapping = new ConcurrentHashMap<>(8);

        /**
         * toString()方法返回该值
         */
        private String string;

        ExtendAnnotationInvocationHandler(Annotation source) {
            this.source = source;
            this.annotationMethods = ContainerUtils.arrayToSet(source.annotationType().getDeclaredMethods());
            this.attributeMethodMap.putAll(annotationMethods.stream().collect(Collectors.toMap(Method::getName, m -> m)));
            initializeExtendAttributeMapping();
        }

        public Annotation getProxyAnnotation() {
            return (Annotation) ProxyFactory.getJdkProxyObject(source.annotationType(), this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (ReflectionUtils.isEqualsMethod(method)) {
                return Objects.equals(proxy, args[0]);
            }
            if (ReflectionUtils.isHashCodeMethod(method)) {
                return proxy.hashCode();
            }
            if (ReflectionUtils.isToStringMethod(method)) {
                return annotationToString();
            }
            if (isAnnotationTypeMethod(method)) {
                return this.source.annotationType();
            }
            if (annotationMethods.contains(method)) {
                return getAnnotationAttribute(method.getName());
            }
            throw new AnnotationConfigurationException(String.format("Method [%s] is unsupported for synthesized annotation type [%s]", method, this.source.annotationType()));
        }

        private void initializeExtendAttributeMapping() {
            for (Method attribute : ClassUtils.getAllMethod(source.annotationType())) {
                ExtendFor extendForAnn = findMergedAnnotation(attribute, ExtendFor.class);
                if (extendForAnn == null) {
                    continue;
                }
                String attributeName = attribute.getName();
                for (String extendName : extendForAnn.value()) {
                    if (extendAttributeMapping.containsKey(extendName)) {
                        throw new IllegalStateException("Illegal annotation attribute extension: The original annotation attribute '" + attributeName + "' and '" + extendAttributeMapping.get(extendName) + "' extend the attribute '" + extendName + "' at the same time.");
                    }
                    extendAttributeMapping.put(extendName, attributeName);
                }
            }
        }


        private Object getAnnotationAttribute(String attributeName) {
            // 注解的原属性
            if (attributeMethodMap.containsKey(attributeName)) {
                Object attributeValue = this.valueMap.computeIfAbsent(attributeName, _name -> getValue(source, _name));
                Object defaultValue = getDefaultValue(attributeName);


                if (Objects.equals(attributeValue, defaultValue) && extendAttributeMapping.containsKey(attributeName)) {
                    attributeValue = getAnnotationAttribute(extendAttributeMapping.get(attributeName));
                }
                valueMap.put(attributeName, attributeValue);
                return attributeValue;
            }
            // 注解的扩展属性
            if (extendAttributeMapping.containsKey(attributeName)) {
                return getAnnotationAttribute(extendAttributeMapping.get(attributeName));
            }

            throw new LuckyReflectionException("The attribute named '{}' is not found in the @'{}' annotation", attributeName, source.annotationType().getName());
        }

        public Object getDefaultValue(String attributeName) {
            return this.defaultValueMap.computeIfAbsent(attributeName, this::doGetDefaultValue);
        }

        public Method getAttributeMethod(String attributeName) {
            if (isSoueceAttribute(attributeName)) {
                return attributeMethodMap.get(attributeName);
            }
            if (isExtendAttribute(attributeName)) {
                return attributeMethodMap.get(extendAttributeMapping.get(attributeName));
            }
            return null;
        }

        public void setValue(String attributeName, Object value) {
            if (isSoueceAttribute(attributeName)) {
                Method attributeMethod = attributeMethodMap.get(attributeName);
                try {
                    this.valueMap.put(attributeName, ConversionUtils.conversion(value, ResolvableType.forMethodReturnType(attributeMethod)));
                    this.string = null;
                } catch (Exception e) {
                    throw new LuckyReflectionException("The {} value {} cannot be assigned to the annotated @{} attribute '{}' of type {}",
                            value.getClass(), value, source.annotationType().getName(), attributeName, attributeMethod.getReturnType().getName()
                    );
                }

            } else if (isExtendAttribute(attributeName)) {
                setValue(extendAttributeMapping.get(attributeName), value);
            } else {
                throw new LuckyReflectionException("The attribute named '{}' is not found in the @'{}' annotation", attributeName, this.source.annotationType().getName());
            }
        }

        private Object doGetDefaultValue(String attributeName) {
            if (isSoueceAttribute(attributeName)) {
                return AnnotationUtils.getDefaultValue(source, attributeName);
            }
            if (isExtendAttribute(attributeName)) {
                return doGetDefaultValue(extendAttributeMapping.get(attributeName));
            }
            return null;
        }

        private boolean isSoueceAttribute(String attributeName) {
            return attributeMethodMap.containsKey(attributeName);
        }

        private boolean isExtendAttribute(String attributeName) {
            return extendAttributeMapping.containsKey(attributeName);
        }

        private boolean isAnnotationTypeMethod(Method method) {
            return (method.getName().equals("annotationType") && method.getParameterCount() == 0);
        }

        private String annotationToString() {
            String string = this.string;
            if (string == null) {
                String header = "@" + StringUtils.getClassName(this.source.annotationType()) + "(";
                String tail = ")";

                Set<String> atributeSet = new LinkedHashSet<>(attributeMethodMap.keySet());
                atributeSet.addAll(extendAttributeMapping.keySet());
                List<String> body = new ArrayList<>(atributeSet.size());

                for (String attribute : atributeSet) {
                    body.add(attribute + "=" + StringUtils.toString(getAnnotationAttribute(attribute)));
                }
                string = header + StringUtils.join(body, ", ") + tail;
                this.string = string;
            }
            return string;
        }


    }

    /**
     * 注解类型转换拦截器
     */
    static final class AnnotationTypeConversionInvocationHandler implements InvocationHandler {

        /**
         * 原注解
         */
        private final Annotation sourceAnnotation;

        /**
         * 目标注解类型
         */
        private final Class<? extends Annotation> targetAnnotationType;

        /**
         * 目标注解属性名和对应方法组成的Map
         */
        private final Map<String, Method> targetMethodMap = new ConcurrentHashMap<>(8);

        /**
         * 当前注解属性值缓存
         */
        private final Map<String, Object> targetValueMap = new ConcurrentHashMap<>(8);

        /**
         * toString()方法返回该值
         */
        private String string;

        AnnotationTypeConversionInvocationHandler(Annotation sourceAnnotation, Class<? extends Annotation> targetAnnotationType) {
            this.sourceAnnotation = sourceAnnotation;
            this.targetAnnotationType = targetAnnotationType;
            this.targetMethodMap.putAll(Stream.of(targetAnnotationType.getDeclaredMethods()).collect(Collectors.toMap(Method::getName, m -> m)));
        }

        public Annotation getProxyAnnotation() {
            return (Annotation) ProxyFactory.getJdkProxyObject(targetAnnotationType, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (ReflectionUtils.isEqualsMethod(method)) {
                return Objects.equals(proxy, args[0]);
            }
            if (ReflectionUtils.isHashCodeMethod(method)) {
                return proxy.hashCode();
            }
            if (ReflectionUtils.isToStringMethod(method)) {
                return annotationToString();
            }
            if (isAnnotationTypeMethod(method)) {
                return this.targetAnnotationType;
            }
            if (isAnnotationMethod(method)) {
                return getAnnotationAttribute(method.getName());
            }
            throw new AnnotationConfigurationException(String.format("Method [%s] is unsupported for synthesized annotation type [%s]", method, this.targetAnnotationType));
        }

        private Object getAnnotationAttribute(String attribute) {
            return targetValueMap.computeIfAbsent(attribute, this::doGetAnnotationAttribute);
        }

        private Object doGetAnnotationAttribute(String attribute) {
            ResolvableType sourceAttributeType = AnnotationUtils.getAttributeType(sourceAnnotation, attribute);
            ResolvableType targetAttributeType = ResolvableType.forMethodReturnType(targetMethodMap.get(attribute));

            if (sourceAttributeType != null && ClassUtils.compatibleOrNot(targetAttributeType, sourceAttributeType)) {
                return getValue(sourceAnnotation, attribute);
            }
            Annotation methodTargetAnn = findMergedAnnotation(AnnotationUtils.getAttributeMethod(sourceAnnotation, attribute), targetAnnotationType);
            if (methodTargetAnn != null) {
                return getValue(methodTargetAnn, attribute);
            }
            Annotation classTargetAnn = findMergedAnnotation(sourceAnnotation.annotationType(), targetAnnotationType);
            if (classTargetAnn != null) {
                return getValue(classTargetAnn, attribute);
            }
            return MethodUtils.getDeclaredMethod(targetAnnotationType, attribute).getDefaultValue();
        }

        private boolean isAnnotationTypeMethod(Method method) {
            return (method.getName().equals("annotationType") && method.getParameterCount() == 0);
        }


        private boolean isAnnotationMethod(Method method) {
            return this.targetMethodMap.containsKey(method.getName());
        }

        private Object annotationToString() {
            String string = this.string;
            if (string == null) {
                String header = "@" + StringUtils.getClassName(this.targetAnnotationType) + "(";
                String tail = ")";

                Set<String> atributeSet = new LinkedHashSet<>(targetMethodMap.keySet());
                List<String> body = new ArrayList<>(atributeSet.size());

                for (String attribute : atributeSet) {
                    body.add(attribute + "=" + StringUtils.toString(getAnnotationAttribute(attribute)));
                }
                string = header + StringUtils.join(body, ", ") + tail;
                this.string = string;
            }
            return string;
        }

    }

}
