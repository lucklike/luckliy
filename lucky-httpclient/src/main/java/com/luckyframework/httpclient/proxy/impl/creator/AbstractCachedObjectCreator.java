package com.luckyframework.httpclient.proxy.impl.creator;

import com.luckyframework.cache.Cache;
import com.luckyframework.cache.impl.ConcurrentMapCache;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.ObjectCreator;

/**
 * 带有缓存功能的对象创建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/31 10:10
 */
public abstract class AbstractCachedObjectCreator implements ObjectCreator {

    private final Cache<String, Object> cache = new ConcurrentMapCache<>(32);

    @Override
    @SuppressWarnings("unchecked")
    public <T> T newObject(Class<T> aClass, String createMessage) {
        String cacheKey = StringUtils.format("[{0}] {1}", createMessage, aClass);
        Object object = cache.get(cacheKey);
        if(object == null) {
            object = createObject(aClass, createMessage);
            cache.put(cacheKey, object);
        }
        return (T) object;
    }

    protected abstract <T> T createObject(Class<T> aClass, String createMessage);

}
