package com.luckyframework.scanner;

import com.luckyframework.common.ScanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;
import java.util.Set;

import static com.luckyframework.scanner.Constants.SCANNER_ELEMENT_ANNOTATION_NAME;

/**
 * 组件自动扫描器
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/19 上午12:42
 */
public class ComponentAutoScanner extends AbstractScanner {
    private final static String[] DEFAULT_PACKAGE = {""};

    public ComponentAutoScanner() {
        scanner(DEFAULT_PACKAGE);
    }

    public ComponentAutoScanner(Class<?> rootClass) {
        this(new Class<?>[]{rootClass});
    }

    public ComponentAutoScanner(Class<?>[] rootClasses) {
        scanner(ScanUtils.getPackages(rootClasses));
    }

    public ComponentAutoScanner(String basePackage) {
        this(new String[]{basePackage});
    }

    public ComponentAutoScanner(String[] basePackages) {
        scanner(basePackages);
    }

    public ComponentAutoScanner(Set<Resource> classResources) {
        scanner(classResources);
    }

    // 扫描所有类资源，并从中得到需要的扫描组件
    private void scanner(Set<Resource> classResources) {
        // 收集类资源中所有的的扫描元素
        classResources.stream()
                .map(ScanUtils::resourceToAnnotationMetadata)
                .filter(am -> ScannerUtils.annotationIsExist(am, SCANNER_ELEMENT_ANNOTATION_NAME))
                .forEach(this::addScannerElement);
        scannerBySpi();
    }

    private void scanner(String[] basePackages) {
        ScanUtils.resourceHandle(basePackages, r -> {
            AnnotationMetadata annotationMetadata = ScanUtils.resourceToAnnotationMetadata(r);
            if (ScannerUtils.annotationIsExist(annotationMetadata, SCANNER_ELEMENT_ANNOTATION_NAME)) {
                addScannerElement(annotationMetadata);
            }
        });
        scannerBySpi();
    }

    // 2.收集由SPI机制导入的组件[META-INF/lucky.factories]
    private void scannerBySpi() {
        List<AnnotationMetadata> spiScannerElements = ScannerUtils.getAnnotationMetadataBySpi();
        spiScannerElements.forEach(this::addScannerElement);
    }
}
