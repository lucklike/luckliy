package com.luckyframework.diff;

import java.util.UUID;

/**
 * 基于UUID实现的ID实体
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 03:13
 */
public class UUIDEntity extends IdEntity {

    @Override
    public String generateId() {
        return UUID.randomUUID().toString().toUpperCase().replace("-", "");
    }

}
