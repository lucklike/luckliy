package com.luckyframework.webmvc.applicationcontext;

import com.luckyframework.context.AnnotationScannerApplicationContext;
import com.luckyframework.scanner.ComponentAutoScanner;
import com.luckyframework.scanner.Scanner;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;
import java.util.Set;

public class WebAnnotationScannerApplicationContext extends AnnotationScannerApplicationContext {

    public WebAnnotationScannerApplicationContext(){
        this(new ComponentAutoScanner());
    }

    public WebAnnotationScannerApplicationContext(Class<?> rootClass){
        this(new ComponentAutoScanner(rootClass));
    }

    public WebAnnotationScannerApplicationContext(String basePackage){
        this(new ComponentAutoScanner(basePackage));
    }

    public WebAnnotationScannerApplicationContext(Class<?>...rootClasses){
        this(new ComponentAutoScanner(rootClasses));
    }

    public WebAnnotationScannerApplicationContext(String...basePackages){
        this(new ComponentAutoScanner(basePackages));
    }

    public WebAnnotationScannerApplicationContext(Scanner scanner) {
        this(scanner.getScannerElements());
    }

    public WebAnnotationScannerApplicationContext(Set<Resource> classResources) {
        this(new ComponentAutoScanner(classResources));
    }

    public WebAnnotationScannerApplicationContext(List<AnnotationMetadata> annotationMetadataList){
        super(annotationMetadataList);
        init();
    }

    public static WebAnnotationScannerApplicationContext create(){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext();
        context.refresh();
        return context;
    }

    public static WebAnnotationScannerApplicationContext create(Class<?> rootClass){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext(rootClass);
        context.refresh();
        return context;
    }

    public static WebAnnotationScannerApplicationContext create(String basePackage){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext(basePackage);
        context.refresh();
        return context;
    }

    public static WebAnnotationScannerApplicationContext create(String...basePackages){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext(basePackages);
        context.refresh();
        return context;
    }


    public static WebAnnotationScannerApplicationContext create(Class<?>...rootClasses){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext(rootClasses);
        context.refresh();
        return context;
    }

    public static WebAnnotationScannerApplicationContext create(Scanner scanner){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext(scanner);
        context.refresh();
        return context;
    }

    public static WebAnnotationScannerApplicationContext create(List<AnnotationMetadata> annotationMetadataList){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext(annotationMetadataList);
        context.refresh();
        return context;
    }

    public static WebAnnotationScannerApplicationContext create(Set<Resource> classResources){
        WebAnnotationScannerApplicationContext context = new WebAnnotationScannerApplicationContext(classResources);
        context.refresh();
        return context;
    }
}
