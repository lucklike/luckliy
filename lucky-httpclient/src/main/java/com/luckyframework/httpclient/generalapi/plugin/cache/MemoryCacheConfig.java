package com.luckyframework.httpclient.generalapi.plugin.cache;

import org.springframework.util.StringUtils;

/**
 * {@link MemoryCacheImpl}的缓存配置
 *
 * <p>默认的配置来源为{@link MemoryCache @MemoryCache}合并注解，需要自定义配置来源时可以继承
 * {@link MemoryCacheImpl}并重写{@link MemoryCacheImpl#getConfig}方法来实现
 *
 * @author fukang
 * @version 3.0.1
 * @date 2026/9/24
 */
public class MemoryCacheConfig {

    /**
     * 默认配置：不限制容量，不保存到磁盘
     */
    public static final MemoryCacheConfig DEFAULT = new MemoryCacheConfig(-1, "");

    /**
     * 缓存的最大容量，小于等于0时表示不限制容量
     */
    private final long capacity;

    /**
     * 缓存数据保存的目录，空白字符串表示不保存到磁盘
     */
    private final String saveDir;

    /**
     * 构造方法
     *
     * @param capacity 缓存的最大容量，小于等于0时表示不限制容量
     * @param saveDir  缓存数据保存的目录，空白字符串表示不保存到磁盘
     */
    public MemoryCacheConfig(long capacity, String saveDir) {
        this.capacity = capacity;
        this.saveDir = saveDir;
    }

    /**
     * 获取缓存的最大容量，小于等于0时表示不限制容量
     *
     * @return 缓存的最大容量
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * 获取缓存数据保存的目录，空白字符串表示不保存到磁盘
     *
     * @return 缓存数据保存的目录
     */
    public String getSaveDir() {
        return saveDir;
    }

    /**
     * 是否开启了磁盘持久化
     *
     * @return true[已开启]/false[未开启]
     */
    public boolean isDiskEnabled() {
        return StringUtils.hasText(saveDir);
    }
}
