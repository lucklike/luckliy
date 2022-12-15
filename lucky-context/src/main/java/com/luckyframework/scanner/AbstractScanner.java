package com.luckyframework.scanner;

import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象的扫描器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/19 上午12:03
 */
public abstract class AbstractScanner implements Scanner{

    /** 所有的扫描元素*/
    private final List<AnnotationMetadata> scannerElementSet = new ArrayList<>(225);

    /**
     * 添加一个扫描元素
     * @param scannerElement 扫描元素
     */
    public void addScannerElement(AnnotationMetadata scannerElement){
        this.scannerElementSet.add(scannerElement);
    }

    @Override
    public List<AnnotationMetadata> getScannerElements() {
        return this.scannerElementSet;
    }
}
