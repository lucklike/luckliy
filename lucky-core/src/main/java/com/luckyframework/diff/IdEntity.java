package com.luckyframework.diff;

import java.io.Serializable;

/**
 * ID实体类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 03:07
 */
public abstract class IdEntity implements Serializable {

    /**
     * ID
     */
    private String id;

    /**
     * 获取ID
     *
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * 将ID刷新为新的
     */
    public void refreshId() {
        this.id = generateId();
    }

    /**
     * 获取ID，如果ID不存在则生成一个返回
     * @return ID
     */
    public String getIdIfNotGenerate() {
        generateIdIfNot();
        return getId();
    }

    /**
     * ID如果不存在，则生成一个ID
     */
    public void generateIdIfNot() {
        if (this.id == null) {
            refreshId();
        }
    }

    /**
     * 生成ID
     *
     * @return ID
     */
    public abstract String generateId();
}
