package com.luckyframework.context;

import com.luckyframework.scanner.ComponentAutoScanner;
import com.luckyframework.scanner.Scanner;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;
import java.util.Set;

/**
 * 基于扫组件注解扫描的应用程序上下文
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/12 上午1:43
 */
public class AnnotationScannerApplicationContext extends AbstractBasedAnnotationApplicationContext {

    public AnnotationScannerApplicationContext(){
        this(new ComponentAutoScanner());
    }

    public AnnotationScannerApplicationContext(Class<?> rootClass){
        this(new ComponentAutoScanner(rootClass));
    }

    public AnnotationScannerApplicationContext(String basePackage){
        this(new ComponentAutoScanner(basePackage));
    }

    public AnnotationScannerApplicationContext(Class<?>...rootClasses){
        this(new ComponentAutoScanner(rootClasses));
    }

    public AnnotationScannerApplicationContext(String...basePackages){
        this(new ComponentAutoScanner(basePackages));
    }

    public AnnotationScannerApplicationContext(Scanner scanner) {
        this(scanner.getScannerElements());
    }

    public AnnotationScannerApplicationContext(List<AnnotationMetadata> annotationMetadataList){
        super(annotationMetadataList);
        init();
    }

    public AnnotationScannerApplicationContext(Set<Resource> classResources) {
        this(new ComponentAutoScanner(classResources));
    }

    public static AnnotationScannerApplicationContext create(){
        AnnotationScannerApplicationContext context = new AnnotationScannerApplicationContext();
        context.refresh();
        return context;
    }

    public static AnnotationScannerApplicationContext create(Class<?> rootClass){
        AnnotationScannerApplicationContext context = new AnnotationScannerApplicationContext(rootClass);
        context.refresh();
        return context;
    }

    public static AnnotationScannerApplicationContext create(String basePackage){
        AnnotationScannerApplicationContext context = new AnnotationScannerApplicationContext(basePackage);
        context.refresh();
        return context;
    }

    public static AnnotationScannerApplicationContext create(Class<?>...rootClasses){
        AnnotationScannerApplicationContext context = new AnnotationScannerApplicationContext(rootClasses);
        context.refresh();
        return context;
    }

    public static AnnotationScannerApplicationContext create(Scanner scanner){
        AnnotationScannerApplicationContext context = new AnnotationScannerApplicationContext(scanner);
        context.refresh();
        return context;
    }

    public static AnnotationScannerApplicationContext create(List<AnnotationMetadata> annotationMetadataList){
        AnnotationScannerApplicationContext context = new AnnotationScannerApplicationContext(annotationMetadataList);
        context.refresh();
        return context;
    }

    public static AnnotationScannerApplicationContext create(Set<Resource> classResources){
        AnnotationScannerApplicationContext context = new AnnotationScannerApplicationContext(classResources);
        context.refresh();
        return context;
    }
}
