package com.luckyframework.httpclient.generalapi.plugin.cache;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerateUtil;
import com.luckyframework.httpclient.proxy.context.ContextAware;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.plugin.PluginException;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;

/**
 * 缓存插件实现类
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
public class CacheProxyPlugin implements ProxyPlugin {

    /**
     * 缓存处理逻辑：
     * <pre>
     *     1.未标注{@link CachePluginMeta}注解时，直接执行原方法
     *     2.解析缓存key并尝试命中缓存，命中时直接返回缓存数据
     *     3.未命中时执行原方法，返回非{@code null}时按配置的过期时间将结果写入缓存
     * </pre>
     *
     * @param decorator 方法装饰器
     * @return 方法执行结果
     * @throws Throwable 执行过程中可能出现的异常
     */
    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {

        ExecuteMeta meta = decorator.getMeta();
        MethodContext mc = meta.getMethodContext();

        // 获取缓存插件注解，未标注该注解时直接执行原方法
        CachePluginMeta cacheAnn = mc.getMergedAnnotationCheckParent(CachePluginMeta.class);
        if (cacheAnn == null) {
            return decorator.proceed();
        }

        // 获取缓存对象，缓存对象实现了ContextAware接口时会注入当前API方法上下文
        ICache cache = getCacheObject(mc, cacheAnn);

        // 解析缓存Key，并尝试命中缓存
        String key = mc.parseExpression(cacheAnn.key(), String.class);
        Object result = cache.get(mc, key);
        if (result != null) {
            return result;
        }

        // 缓存未命中时执行原方法
        result = decorator.proceed();
        if (result == null) {
            return null;
        }

        // 解析过期时间，并将结果写入缓存
        long expires = mc.parseExpression(cacheAnn.expires(), long.class);
        if (expires < 0) {
            cache.put(mc, key, result);
        } else {
            cache.put(mc, key, result, expires);
        }
        return result;
    }

    /**
     * 获取缓存对象
     * <pre>
     *     优先级1：使用{@link CachePluginMeta#cacheGenerate()}指定的对象生成器来生成缓存对象
     *     优先级2：使用{@link CachePluginMeta#cache()}指定的Class来生成缓存对象
     * </pre>
     *
     * @param mc       方法上下文
     * @param cacheAnn 缓存插件注解实例
     * @return 缓存对象
     */
    private ICache getCacheObject(MethodContext mc, CachePluginMeta cacheAnn) {

        // 优先使用对象生成器来生成缓存对象
        ObjectGenerate generate = cacheAnn.cacheGenerate();
        if (ObjectGenerateUtil.isEffectiveObjectGenerate(generate, Void.class)) {
            return checkCacheObject(mc.generateObject(generate));
        }

        // 其次使用Class来生成缓存对象
        Class<? extends ICache> cacheClass = cacheAnn.cache();
        if (cacheClass != null && cacheClass != ICache.class) {
            return checkCacheObject(mc.generateObject(cacheClass, Scope.SINGLETON));
        }

        throw new PluginException("Cache plugin executed exception：No applicable ICache configuration was found.");
    }

    /**
     * 校验缓存对象是否有效
     *
     * @param cache 缓存对象
     * @return 缓存对象
     */
    private ICache checkCacheObject(ICache cache) {
        if (cache == null) {
            throw new PluginException("Cache plugin executed exception：cache object is null");
        }
        return cache;
    }

}
