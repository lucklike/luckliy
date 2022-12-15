package com.luckyframework.scanner;

import com.luckyframework.common.ContainerUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.luckyframework.scanner.Constants.SCANNER_ELEMENT_ANNOTATION_NAME;

/**
 * 组件自动扫描器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/19 上午12:42
 */
public class ComponentAutoScanner extends AbstractScanner{

    private final static CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
    private final static String PATH_PREFIX = "classpath*:";
    private final static String PATH_SUFFIX = "/**/*.class";
    private final static String[] DEFAULT_PACKAGE = {""};

    public ComponentAutoScanner(){
        scanner(new String[]{""});
    }

    public ComponentAutoScanner(Class<?> rootClass){
        this(new Class<?>[]{rootClass});
    }

    public ComponentAutoScanner(Class<?>[] rootClasses){
        scanner(getPackages(rootClasses));
    }

    public ComponentAutoScanner(String basePackage){
        this(new String[]{basePackage});
    }

    public ComponentAutoScanner(String[] basePackages){
        scanner(basePackages);
    }

    public ComponentAutoScanner(Set<Resource> classResources){
        scanner(classResources);
    }

    // 扫描所有类资源，并从中得到需要的扫描组件
    private void scanner(Set<Resource> classResources){
        try {
            // 收集类资源中所有的的扫描元素
            for (Resource resource : classResources) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
                if(ScannerUtils.annotationIsExist(annotationMetadata,SCANNER_ELEMENT_ANNOTATION_NAME)){
                    addScannerElement(annotationMetadata);
                }
            }
            // 2.收集由SPI机制导入的组件[META-INF/lucky.factories]
            List<AnnotationMetadata> spiScannerElements = ScannerUtils.getAnnotationMetadataBySpi();
            spiScannerElements.forEach(this::addScannerElement);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void scanner(String[] basePackages){
        try {
            Set<Resource> classResources = new HashSet<>();
            // 搜集所有class资源
            basePackages = ContainerUtils.isEmptyArray(basePackages) ? DEFAULT_PACKAGE : basePackages;
            for (String basePackage : basePackages) {
                String packagePath = basePackage.replaceAll("\\.", "/");
                Resource[] resources = PM.getResources(PATH_PREFIX+packagePath+PATH_SUFFIX);
                classResources.addAll(Arrays.asList(resources));
            }
            // 将所有class资源转化为扫描元素
            scanner(classResources);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] getPackages(Class<?>[] rootClass){
        if(rootClass == null || rootClass.length == 0){
            return new String[]{""};
        }
        String[] basePackages = new String[rootClass.length];
        for (int i = 0; i < rootClass.length; i++) {
            Package aPackage = rootClass[i].getPackage();
            basePackages[i] = aPackage==null?"":aPackage.getName();
        }
        return basePackages;
    }
}
