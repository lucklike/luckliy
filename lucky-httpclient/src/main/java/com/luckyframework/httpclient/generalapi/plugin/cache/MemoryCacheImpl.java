package com.luckyframework.httpclient.generalapi.plugin.cache;

import com.luckyframework.cache.finder.ExpiringMap;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * {@link ICache}的进程内缓存实现，也是{@link MemoryCache @MemoryCache}注解默认使用的缓存实现
 *
 * <p>底层使用{@link ExpiringMap}存储缓存数据，采用"惰性删除 + 周期清理"的方式回收过期条目：
 * <pre>
 *     惰性删除：读取时若命中的条目已过期，则会立即将其移除并返回{@code null}
 *     周期清理：后台清理线程会周期性地清理已经过期的条目
 * </pre>
 *
 * <p>此外还支持以下两种能力：
 * <pre>
 *     1.容量控制：通过{@link MemoryCache#capacity()}配置最大容量，容量超限时按最近最少使用(LRU)策略淘汰数据
 *     2.磁盘持久化：通过{@link MemoryCache#saveDir()}配置保存目录后，缓存数据会以JSON格式持久化到磁盘，
 *       应用启动后首次访问时自动从磁盘恢复数据，已过期或者被移除的数据文件会被同步清理
 *       (要求缓存数据支持JSON序列化，不满足时会跳过落盘处理并输出警告)
 * </pre>
 *
 * <p>配置默认从{@link MemoryCache @MemoryCache}合并注解中读取，需要自定义配置来源时可以继承本类并重写
 * {@link #getConfig(MethodContext)}方法来实现
 *
 * <p>该实现只提供进程内的缓存能力，无法在分布式环境下共享缓存数据；
 * 缓存对象使用{@link com.luckyframework.httpclient.proxy.creator.Scope#SINGLETON}生成时，
 * 同一个Class只会存在一个实例，此时缓存数据在整个应用内共享，
 * 容量以及保存目录均以首次初始化时解析到的配置为准
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
public class MemoryCacheImpl implements ICache {

    private static final Logger logger = LoggerFactory.getLogger(MemoryCacheImpl.class);

    /**
     * 磁盘缓存文件的后缀
     */
    private static final String DISK_FILE_SUFFIX = ".cache";

    /**
     * 缓存数据存储器
     */
    private final ExpiringMap<String, Object> cache = new ExpiringMap<>();

    /**
     * 最近最少使用(LRU)的顺序表，仅在开启容量限制时维护
     */
    private final LinkedHashMap<String, Boolean> lruMap = new LinkedHashMap<>(16, 0.75F, true);

    /**
     * 是否已经完成初始化
     */
    private volatile boolean initialized = false;

    /**
     * 缓存的最大容量，小于等于0时表示不限制容量
     */
    private long capacity = -1;

    /**
     * 缓存数据保存的目录，{@code null}表示未开启磁盘持久化
     */
    private File saveDirectory = null;

    /**
     * 获取缓存数据，未命中或者已过期时返回{@code null}
     *
     * <p>内存中未命中时会尝试从磁盘恢复数据，已过期或者损坏的数据文件会被清理
     *
     * @param mc  当前API方法对应的方法上下文
     * @param key 缓存key
     * @return 缓存数据
     */
    @Override
    public Object get(MethodContext mc, String key) {
        ensureInitialized(mc);
        Object value = cache.getNotExpired(key);
        if (value != null) {
            recordAccess(key);
            return value;
        }
        return restoreFromDisk(mc, key);
    }

    /**
     * 写入缓存，写入的条目永不过期
     *
     * @param mc    当前API方法对应的方法上下文
     * @param key   缓存key
     * @param value 缓存数据
     */
    @Override
    public void put(MethodContext mc, String key, Object value) {
        ensureInitialized(mc);
        storeInMemory(key, value, -1);
        writeToDisk(key, value, -1);
    }

    /**
     * 写入缓存，并指定过期时间
     *
     * @param mc      当前API方法对应的方法上下文
     * @param key     缓存key
     * @param value   缓存数据
     * @param expires 过期时间，单位：毫秒，小于0时表示永不过期
     */
    @Override
    public void put(MethodContext mc, String key, Object value, long expires) {
        ensureInitialized(mc);
        long expiredMillis = expires < 0 ? -1 : System.currentTimeMillis() + expires;
        storeInMemory(key, value, expiredMillis);
        writeToDisk(key, value, expiredMillis);
    }

    /**
     * 移除缓存，内存以及磁盘中的数据都会被移除
     *
     * @param mc  当前API方法对应的方法上下文
     * @param key 缓存key
     */
    @Override
    public void remove(MethodContext mc, String key) {
        ensureInitialized(mc);
        cache.remove(key);
        synchronized (lruMap) {
            lruMap.remove(key);
        }
        deleteDiskFile(key);
    }

    //------------------------------------------------------------------------
    //                           初始化相关
    //------------------------------------------------------------------------

    /**
     * 获取缓存配置，默认从{@link MemoryCache @MemoryCache}合并注解中解析以下配置：
     * <pre>
     *     1.{@link MemoryCache#capacity()}：缓存的最大容量
     *     2.{@link MemoryCache#saveDir()}：缓存数据保存的目录
     * </pre>
     *
     * <p>子类可以重写该方法来实现自定义的配置来源，例如从其他注解或者外部配置中读取配置
     *
     * @param mc 当前API方法对应的方法上下文
     * @return 缓存配置
     */
    protected MemoryCacheConfig getConfig(MethodContext mc) {
        MemoryCache memoryCacheAnn = mc.getMergedAnnotationCheckParent(MemoryCache.class);
        if (memoryCacheAnn == null) {
            return MemoryCacheConfig.DEFAULT;
        }
        return new MemoryCacheConfig(
                mc.parseExpression(memoryCacheAnn.capacity(), long.class),
                mc.parseExpression(memoryCacheAnn.saveDir(), String.class));
    }

    /**
     * 首次使用时完成初始化：应用{@link #getConfig(MethodContext)}中获取到的缓存配置，
     * 配置了保存目录时会创建对应的目录，之后的缓存数据会以JSON格式写入该目录
     *
     * @param mc 当前API方法对应的方法上下文
     */
    private void ensureInitialized(MethodContext mc) {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            applyConfig(getConfig(mc));
            this.initialized = true;
            logger.debug("MemoryCacheImpl initialized, capacity: {}, saveDir: {}", this.capacity, this.saveDirectory);
        }
    }

    /**
     * 应用缓存配置
     *
     * @param config 缓存配置
     */
    private void applyConfig(MemoryCacheConfig config) {
        this.capacity = config.getCapacity();
        if (config.isDiskEnabled()) {
            this.saveDirectory = new File(config.getSaveDir());
            createSaveDirectory();
        }
    }

    /**
     * 创建磁盘保存目录
     */
    private void createSaveDirectory() {
        try {
            Files.createDirectories(saveDirectory.toPath());
        } catch (IOException e) {
            throw new LuckyRuntimeException(e, "Failed to create the cache save directory '{}'", saveDirectory).error(logger);
        }
    }

    //------------------------------------------------------------------------
    //                          内存层操作
    //------------------------------------------------------------------------

    /**
     * 将数据写入内存缓存，并按需执行容量淘汰
     *
     * @param key           缓存key
     * @param value         缓存数据
     * @param expiredMillis 过期时间(绝对毫秒时间戳)，小于0表示永不过期
     */
    private void storeInMemory(String key, Object value, long expiredMillis) {
        if (expiredMillis < 0) {
            cache.put(key, value);
        } else {
            cache.put(key, value, expiredMillis);
        }
        recordAccess(key);
        evictIfOverCapacity();
    }

    /**
     * 记录一次访问，用于LRU淘汰
     *
     * @param key 缓存key
     */
    private void recordAccess(String key) {
        if (capacity <= 0) {
            return;
        }
        synchronized (lruMap) {
            lruMap.put(key, Boolean.TRUE);
        }
    }

    /**
     * 容量超限时按LRU策略淘汰数据，内存以及磁盘中的数据都会被移除。
     *
     * <p>由于LRU顺序表中可能存在已经过期或者被移除的key，这里采用近似的淘汰策略：
     * 不断从顺序表头部取出key并从内存和磁盘中移除，直到顺序表大小不大于容量为止，
     * 而未过期的条目一定存在于顺序表中，因此该策略可以保证内存中的条目数量不会超过容量
     */
    private void evictIfOverCapacity() {
        if (capacity <= 0) {
            return;
        }
        while (true) {
            String evictedKey;
            synchronized (lruMap) {
                if (lruMap.size() <= capacity) {
                    return;
                }
                Iterator<String> iterator = lruMap.keySet().iterator();
                evictedKey = iterator.next();
                iterator.remove();
            }
            cache.remove(evictedKey);
            deleteDiskFile(evictedKey);
        }
    }

    //------------------------------------------------------------------------
    //                           磁盘层操作
    //------------------------------------------------------------------------

    /**
     * 尝试从磁盘恢复缓存数据，数据不存在或者已过期时返回{@code null}，已过期的数据文件会被清理。
     *
     * <p>恢复的数据会以当前API方法的返回值类型进行JSON反序列化
     *
     * @param mc  当前API方法对应的方法上下文
     * @param key 缓存key
     * @return 缓存数据
     */
    private Object restoreFromDisk(MethodContext mc, String key) {
        if (saveDirectory == null) {
            return null;
        }
        File cacheFile = diskFile(key);
        DiskEntry<?> entry = readDiskEntry(mc, cacheFile);
        if (entry == null) {
            return null;
        }

        // md5值发生碰撞时视为未命中，此时不能删除文件(文件中保存的是其他key的数据)
        if (!key.equals(entry.getKey())) {
            logger.warn("A cache file hash collision was detected, the expected key: '{}', the actual key: '{}', this access will be treated as a cache miss.", key, entry.getKey());
            return null;
        }

        // 已过期的数据文件会被清理
        if (entry.isExpired(System.currentTimeMillis())) {
            deleteFileQuietly(cacheFile);
            return null;
        }

        storeInMemory(key, entry.getValue(), entry.getExpireAt());
        return entry.getValue();
    }

    /**
     * 将缓存数据以JSON格式写入磁盘，写入失败时会跳过落盘处理并输出警告
     *
     * @param key           缓存key
     * @param value         缓存数据
     * @param expiredMillis 过期时间(绝对毫秒时间戳)，小于0表示永不过期
     */
    private void writeToDisk(String key, Object value, long expiredMillis) {
        if (saveDirectory == null) {
            return;
        }
        File cacheFile = diskFile(key);
        try {
            String json = JSON_SCHEME.serialization(new DiskEntry<>(key, expiredMillis, value));
            Files.write(cacheFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.warn("An exception occurred while writing the cache file '{}'.", cacheFile, e);
        }
    }

    /**
     * 读取磁盘缓存文件，文件不存在或者内容损坏时返回{@code null}，损坏的文件会被清理。
     *
     * <p>缓存数据会以当前API方法的返回值类型进行JSON反序列化
     *
     * @param mc        当前API方法对应的方法上下文
     * @param cacheFile 磁盘缓存文件
     * @return 磁盘缓存数据条目
     */
    private DiskEntry<?> readDiskEntry(MethodContext mc, File cacheFile) {
        if (!cacheFile.isFile()) {
            return null;
        }
        try {
            String json = new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8);
            Type entryType = ResolvableType.forClassWithGenerics(DiskEntry.class, mc.getReturnResolvableType()).getType();
            return (DiskEntry<?>) JSON_SCHEME.deserialization(json, entryType);
        } catch (Exception e) {
            logger.warn("An exception occurred while reading the cache file '{}', the file will be deleted.", cacheFile, e);
            deleteFileQuietly(cacheFile);
            return null;
        }
    }

    /**
     * 删除key对应的磁盘缓存文件
     *
     * @param key 缓存key
     */
    private void deleteDiskFile(String key) {
        if (saveDirectory == null) {
            return;
        }
        deleteFileQuietly(diskFile(key));
    }

    /**
     * 删除文件，删除失败时仅输出警告
     *
     * @param cacheFile 磁盘缓存文件
     */
    private void deleteFileQuietly(File cacheFile) {
        try {
            Files.deleteIfExists(cacheFile.toPath());
        } catch (IOException e) {
            logger.warn("Failed to delete the cache file '{}'.", cacheFile, e);
        }
    }

    /**
     * 获取key对应的磁盘缓存文件，使用key的md5值作为文件名以避免出现非法字符
     *
     * @param key 缓存key
     * @return 磁盘缓存文件
     */
    private File diskFile(String key) {
        String fileName = DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8)) + DISK_FILE_SUFFIX;
        return new File(saveDirectory, fileName);
    }

    /**
     * 磁盘缓存数据条目
     *
     * @param <T> 缓存数据的类型
     */
    private static class DiskEntry<T> {

        /**
         * 原始的缓存key，用于md5碰撞校验
         */
        private String key;

        /**
         * 过期时间(绝对毫秒时间戳)，小于0表示永不过期
         */
        private long expireAt;

        /**
         * 缓存数据
         */
        private T value;

        DiskEntry() {
        }

        DiskEntry(String key, long expireAt, T value) {
            this.key = key;
            this.expireAt = expireAt;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(long expireAt) {
            this.expireAt = expireAt;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        boolean isExpired(long currentMillis) {
            return expireAt >= 0 && currentMillis > expireAt;
        }
    }
}
