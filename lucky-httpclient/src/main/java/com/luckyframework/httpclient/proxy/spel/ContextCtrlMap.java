package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ModifiedVerifier;
import com.luckyframework.common.VerifierCtrlMap;
import com.luckyframework.httpclient.proxy.context.Context;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于上下文实现的受控Map
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/24 13:36
 */
public abstract class ContextCtrlMap extends VerifierCtrlMap<String, Object> {

    protected final Context context;

    public ContextCtrlMap(Context context, ModifiedVerifier<String> errVerifier, ModifiedVerifier<String> ignoreVerifier) {
        super(new ConcurrentHashMap<>(64), errVerifier, ignoreVerifier);
        this.context = context;
    }

    @Override
    protected boolean existenceOrNot(String key) {
        if (context == null) {
            return delegate.containsKey(key);
        }
        Context temp = context;
        while (temp != null) {
            if (contextHasKey(temp, key)) {
                return true;
            }
            temp = temp.getParentContext();
        }
        return false;
    }

    /**
     * 在上下文对象中是否存在该名称的变量
     *
     * @param context 上下文对象
     * @param key     带校验的变量名
     * @return 是否已经存在
     */
    protected abstract boolean contextHasKey(Context context, String key);

}
