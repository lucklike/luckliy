package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.common.StringUtils;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/4 01:33
 */
public class ObjectGenerateUtil {

    public static boolean isEffectiveObjectGenerate(ObjectGenerate objectGenerate, Class<?> defClazz) {
        if (objectGenerate == null) {
            return false;
        }
        String msg = objectGenerate.msg();
        if (StringUtils.hasText(msg)) {
            return true;
        }

        return defClazz != objectGenerate.clazz();
    }
}
