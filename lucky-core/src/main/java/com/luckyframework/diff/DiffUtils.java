package com.luckyframework.diff;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 差异对象信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/22 07:13
 */
public class DiffUtils<T> {

    /** 住实体是否有差异*/
    private T diffMainEntity;
    private Map<String, Difference<T>> diffMap = new LinkedHashMap<>();

    public void setDiffMainEntity(T diffMainEntity) {
        this.diffMainEntity = diffMainEntity;
    }

    public void addDiff(String fieldName, Difference<T> diff) {
        this.diffMap.put(fieldName, diff);
    }

    public boolean mainEntityHasDiff(){
        return diffMainEntity != null;
    }


}
