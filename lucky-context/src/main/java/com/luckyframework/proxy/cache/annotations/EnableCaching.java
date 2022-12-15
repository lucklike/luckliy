package com.luckyframework.proxy.cache.annotations;

import com.luckyframework.annotations.Import;
import com.luckyframework.proxy.cache.CacheBeanFactoryPostProcessor;
import com.luckyframework.proxy.cache.CacheManagerConfiguration;

import java.lang.annotation.*;

/**
 * 缓存功能开关
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 11:46
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({CacheBeanFactoryPostProcessor.class, CacheManagerConfiguration.class})
public @interface EnableCaching {
}
