package com.luckyframework.junit.core;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.AbstractApplicationContext;
import com.luckyframework.context.AnnotationScannerApplicationContext;
import com.luckyframework.junit.annoations.TestConfiguration;
import com.luckyframework.scanner.ComponentAutoScanner;
import com.luckyframework.scanner.Scanner;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/18 3:00 下午
 */
public class LuckyExtension extends LuckyExtensionAdapter{

    private static final RuntimeMXBean mxb = ManagementFactory.getRuntimeMXBean();
    private AbstractApplicationContext applicationContext;
    static {
        String pid = mxb.getName().split("@")[0];
        MDC.put("pid",pid);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        applicationContext.close();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Class<?> testClass = extensionContext.getTestClass().get();
        TestConfiguration mergedAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, TestConfiguration.class);
        if(mergedAnnotation == null){
            Scanner scanner = new ComponentAutoScanner(testClass);
            List<AnnotationMetadata> scannerElements = scanner.getScannerElements();
            addTestClassAnnotationMetadata(scannerElements, testClass);
            applicationContext = new AnnotationScannerApplicationContext(scannerElements);
        }else{
            Class<?>[] rootClasses = mergedAnnotation.rootClasses();
            String[] basePackages = mergedAnnotation.basePackages();
            Set<String> basePackageSet = new HashSet<>();
            basePackageSet.addAll(Arrays.asList(basePackages));
            basePackageSet.addAll(Stream.of(rootClasses).map(this::getPackageName).collect(Collectors.toList()));
            Scanner scanner = ContainerUtils.isEmptyCollection(basePackageSet)
                            ? new ComponentAutoScanner(testClass)
                            : new ComponentAutoScanner(basePackageSet.toArray(new String[0]));
            List<AnnotationMetadata> scannerElements = scanner.getScannerElements();
            addTestClassAnnotationMetadata(scannerElements, testClass);
            applicationContext = new AnnotationScannerApplicationContext(scannerElements);
        }
        applicationContext.refresh();
    }

    @Override
    public Object createTestInstance(TestInstanceFactoryContext testInstanceFactoryContext, ExtensionContext extensionContext) throws TestInstantiationException {
        Class<?> testClass = extensionContext.getTestClass().get();
        return applicationContext.getBean(testClass);

    }

    private String getPackageName(Class<?> aClass){
        Package aPackage = aClass.getPackage();
        return aPackage==null?"":aPackage.getName();
    }

    private void addTestClassAnnotationMetadata(List<AnnotationMetadata> scannerElements, Class<?> testClass){
        boolean isExist = false;
        String testClassName = testClass.getName();
        for (AnnotationMetadata scannerElement : scannerElements) {
            if(scannerElement.getClassName().equals(testClassName)){
                isExist = true;
                break;
            }
        }
        if(!isExist){
            scannerElements.add(AnnotationMetadata.introspect(testClass));
        }
    }

}
