package com.luckyframework.diff;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/24 09:29
 */
public class NanaIdEntity extends IdEntity {
    @Override
    public String generateId() {
        return NanoIdUtils.randomNanoId();
    }
}
