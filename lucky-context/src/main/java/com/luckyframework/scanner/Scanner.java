package com.luckyframework.scanner;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import java.util.List;

/**
 * 扫描器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/18 下午11:41
 */
public interface Scanner {

    PathMatchingResourcePatternResolver PM = new PathMatchingResourcePatternResolver();

    CachingMetadataReaderFactory METADATA_READER_FACTORY = new CachingMetadataReaderFactory();

    /**
     * 获取所有扫描元素
     */
    List<AnnotationMetadata> getScannerElements();

}
