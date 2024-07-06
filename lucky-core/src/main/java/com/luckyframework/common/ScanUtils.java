package com.luckyframework.common;

import com.luckyframework.exception.LuckyRuntimeException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 扫描相关的工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/29 15:50
 */
public abstract class ScanUtils {

    /**
     * 包名分隔符
     */
    public static final String PACKAGE_SEPARATOR = ".";

    /**
     * 扫描前缀
     */
    private final static String PATH_PREFIX = "classpath*:";

    /**
     * 扫描后缀
     */
    private final static String PATH_SUFFIX = "/**/*.class";

    /**
     * 默认扫描的包名
     */
    private final static String[] DEFAULT_PACKAGE = {""};

    private final static PathMatchingResourcePatternResolver PM = new PathMatchingResourcePatternResolver();
    private final static CachingMetadataReaderFactory METADATA_READER_FACTORY = new CachingMetadataReaderFactory();

    public static String[] exclude(String[] packages1, String[] packages2) {
        List<String> excludeList = new ArrayList<>();
        List<String> list = Arrays.asList(packages1);
        for (String pack : packages2) {
            if (isContain(list, pack)) {
                excludeList.add(pack);
            }
        }
        return excludeList.toArray(new String[0]);
    }

    /**
     * 将{@link Class}数组转化为包名
     *
     * @param classes {@link Class}数组
     * @return 对应的包名数组
     */
    public static String[] getPackages(Class<?>[] classes) {
        if (ContainerUtils.isEmptyArray(classes)) {
            return new String[0];
        }
        return getPackages(classesToPackages(classes));
    }

    public static String[] getPackages(String[] packages) {
        if (ContainerUtils.isEmptyArray(packages)) {
            return new String[0];
        }
        List<String> resultList = new ArrayList<>();
        Stream.of(packages)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparingInt(ScanUtils::getPackageHierarchy))
                .forEach(pn -> {
                    if (!isContain(resultList, pn)) {
                        resultList.add(pn);
                    }
                });
        return resultList.toArray(new String[0]);
    }

    public static String[] getPackages(Class<?>[] classes, String[] packages) {
        List<String> finalPackages = new ArrayList<>();
        if (!ContainerUtils.isEmptyArray(classes)) {
            finalPackages.addAll(Arrays.asList(classesToPackages(classes)));
        }
        if (!ContainerUtils.isEmptyArray(packages)) {
            finalPackages.addAll(Arrays.asList(packages));
        }
        return getPackages(finalPackages.toArray(new String[0]));
    }

    private static String[] classesToPackages(Class<?>[] classes) {
        if (ContainerUtils.isEmptyArray(classes)) {
            return new String[0];
        }
        return Stream.of(classes)
                .filter(Objects::nonNull)
                .map(c -> c.getPackage() == null ? "" : c.getPackage().getName())
                .toArray(String[]::new);
    }

    /**
     * 获取包名的层数
     * <pre>
     *     com.lucky.httpclient  -> 3
     *     com.lucky             -> 2
     *     com                   -> 1
     *     ''                    -> 0
     * </pre>
     *
     * @param packageName 包名
     * @return 层数
     */
    public static int getPackageHierarchy(String packageName) {
        if (!StringUtils.hasText(packageName)) {
            return 0;
        }
        if (!packageName.contains(PACKAGE_SEPARATOR)) {
            return 1;
        }
        return packageName.split("\\.").length;
    }

    /**
     * 判断一个包是否被包含在另外的一系列包中
     *
     * @param packages  包名集合
     * @param checkName 待校验的包的包名
     * @return 一个包是否被包含在另外的一系列包中
     */
    public static boolean isContain(Collection<String> packages, String checkName) {
        for (String aPackage : packages) {
            if (isContain(aPackage, checkName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断两个包之间是否存在包含关系
     *
     * @param parentPackageName 父包包名
     * @param childPackageName  子包包名
     * @return 是否存在包含关系
     */
    public static boolean isContain(String parentPackageName, String childPackageName) {
        parentPackageName = parentPackageName + PACKAGE_SEPARATOR;
        childPackageName = childPackageName + PACKAGE_SEPARATOR;
        return childPackageName.startsWith(parentPackageName);
    }

    public static <T> Set<T> resourceConvert(String[] packages, Function<Resource, T> convert) {
        Set<T> resultSet = new HashSet<>();
        String[] basePackages = ContainerUtils.isEmptyArray(packages) ? DEFAULT_PACKAGE : packages;
        for (String basePackage : basePackages) {
            String packagePath = null;
            try {
                packagePath = basePackage.replaceAll("\\.", "/");
                packagePath = PATH_PREFIX + packagePath + PATH_SUFFIX;
                for (Resource resource : PM.getResources(packagePath)) {
                    T result = convert.apply(resource);
                    if (result != null) {
                        resultSet.add(result);
                    }
                }
            } catch (IOException e) {
                throw new LuckyRuntimeException(e, "Unable to get class resource from path {}", packagePath);
            }
        }
        return resultSet;
    }

    public static AnnotationMetadata resourceToAnnotationMetadata(Resource resource) {
        try {
            return METADATA_READER_FACTORY.getMetadataReader(resource).getAnnotationMetadata();
        } catch (IOException e) {
            throw new LuckyRuntimeException(e, "Unable to convert resource {} to AnnotationMetadata instance.", resource);
        }
    }

    public static void resourceHandle(String[] packages, Consumer<Resource> consumer) {
        String[] basePackages = ContainerUtils.isEmptyArray(packages) ? DEFAULT_PACKAGE : packages;
        for (String basePackage : basePackages) {
            String packagePath = null;
            try {
                packagePath = basePackage.replaceAll("\\.", "/");
                packagePath = PATH_PREFIX + packagePath + PATH_SUFFIX;
                for (Resource resource : PM.getResources(packagePath)) {
                    consumer.accept(resource);
                }
            } catch (IOException e) {
                throw new LuckyRuntimeException(e, "Unable to get class resource from path {}", packagePath);
            }
        }
    }


    public static class ResourceToAnnotationMetadata implements Function<Resource, AnnotationMetadata> {

        private final String annotationName;

        public ResourceToAnnotationMetadata(String annotationName) {
            this.annotationName = annotationName;
        }

        public ResourceToAnnotationMetadata(Class<? extends Annotation> annotationClass) {
            this(annotationClass.getName());
        }


        @Override
        public AnnotationMetadata apply(Resource resource) {
            try {
                AnnotationMetadata annotationMetadata = METADATA_READER_FACTORY.getMetadataReader(resource).getAnnotationMetadata();
                if (annotationMetadata.isAnnotated(annotationName)) {
                    return annotationMetadata;
                }
                return null;
            } catch (IOException e) {
                throw new LuckyRuntimeException(e, "Unable to convert resource {} to AnnotationMetadata instance.", resource);
            }
        }

    }

}
