package com.luckyframework.common;

import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.exception.SnowflakeClockBackwardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * 雪花算法 ID 生成器（分布式生产级 · 时间可读前缀）
 * <p>
 * 生成的 ID 为 25 位纯数字字符串，结构如下：
 * <pre>
 * | yyyyMMddHHmmss | SSS  | WWWW     | SSSS     |
 * |  14位秒级时间    | 3位毫秒 | 4位机器ID | 4位序列号 |
 * </pre>
 * 示例：2026-10-12 16:34:55.123，机器ID=77，序列号=42 生成：
 * <pre>
 * 2026101216345512300770042
 * </pre>
 * 容量：单节点每毫秒最多 10000 个 ID（约 1000 万 ID/秒），分布式最多 10000 个节点。
 * ID 为定长纯数字字符串，字典序即生成先后顺序（同节点严格递增），
 * 且天然规避 long 型 ID 在 JavaScript 中的精度丢失问题。
 * <p>
 * 分布式生产级特性：
 * <pre>
 * 1. 无锁 CAS：毫秒时间戳与序列号打包进一个 AtomicLong，一次 CAS 原子更新两个字段
 * 2. 时钟回拨保护：容忍上限（默认1000ms，可配置）内等待时钟追平；
 *    超过上限抛 {@link SnowflakeClockBackwardException}，绝不生成可能重复的 ID
 * 3. 机器ID分配：JVM参数 > 环境变量 > 物理网卡MAC稳定推导 > 随机兜底（带告警日志）
 * 4. 序列号用尽时自动等待进入下一毫秒，序列号复位时 CAS 竞争安全
 * </pre>
 * 机器ID显式配置方式（二选一）：
 * <pre>
 * -Dlucky.snowflake.workerId=12
 * LUCKY_SNOWFLAKE_WORKER_ID=12
 * </pre>
 * 时钟回拨容忍上限配置方式（二选一，单位毫秒，默认1000）：
 * <pre>
 * -Dlucky.snowflake.maxClockBackwardMs=2000
 * LUCKY_SNOWFLAKE_MAX_CLOCK_BACKWARD_MS=2000
 * </pre>
 * 使用示例：
 * <pre>
 * SnowflakeIdGeneratorCas generator = new SnowflakeIdGeneratorCas(1);
 * String id = generator.nextId();
 * </pre>
 */
public class SnowflakeIdGeneratorCas {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGeneratorCas.class);

    // ==================== ID 格式定义 ====================
    /** 秒级时间前缀位数：yyyyMMddHHmmss */
    private static final int SECOND_TIME_DIGITS = 14;
    /** 毫秒位数 */
    private static final int MILLIS_DIGITS = 3;
    /** 机器ID位数 */
    private static final int WORKER_ID_DIGITS = 4;
    /** 序列号位数 */
    private static final int SEQUENCE_DIGITS = 4;
    /** ID 总长度 */
    public static final int ID_LENGTH = SECOND_TIME_DIGITS + MILLIS_DIGITS + WORKER_ID_DIGITS + SEQUENCE_DIGITS;

    /** 机器ID最大值：9999 */
    private static final long MAX_WORKER_ID = 9999L;
    /** 序列号最大值：9999（单节点每毫秒最多 10000 个 ID） */
    private static final long MAX_SEQUENCE = 9999L;

    /** 序列号占用的二进制位数（16位可表达 0-9999，毫秒时间戳左移该位数存储） */
    private static final int SEQUENCE_BITS = 16;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1L;

    // ==================== 配置项 ====================
    private static final String WORKER_ID_PROPERTY = "lucky.snowflake.workerId";
    private static final String WORKER_ID_ENV = "LUCKY_SNOWFLAKE_WORKER_ID";
    private static final String MAX_BACKWARD_PROPERTY = "lucky.snowflake.maxClockBackwardMs";
    private static final String MAX_BACKWARD_ENV = "LUCKY_SNOWFLAKE_MAX_CLOCK_BACKWARD_MS";
    /** 时钟回拨默认容忍上限（毫秒），超过则抛异常拒绝生成 */
    private static final long DEFAULT_MAX_CLOCK_BACKWARD_MS = 1000L;

    private static final DateTimeFormatter SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final char[] DIGITS = "0123456789".toCharArray();
    private static final int[] POWERS_OF_TEN = {1, 10, 100, 1000, 10000};

    /** 全局默认实例：workerId 按「JVM参数 > 环境变量 > MAC推导 > 随机」解析 */
    public static final SnowflakeIdGeneratorCas INSTANCE = new SnowflakeIdGeneratorCas();

    // ==================== 实例属性 ====================
    private final long workerId;
    /** 4 位补零的机器ID字符串（构造时固化，生成时直接拼接） */
    private final String workerIdSegment;
    private final long maxClockBackwardMs;

    // 打包状态：毫秒时间戳 << 16 | 序列号，一次 CAS 同时更新两个字段
    private final AtomicLong lastTimestampAndSequence = new AtomicLong(0);
    // 秒级时间前缀缓存：同一秒内所有 ID 复用同一个前缀字符串，秒变化时重新格式化
    private final AtomicReference<SecondPrefix> secondPrefixRef = new AtomicReference<>();

    // ==================== 统计信息 ====================
    private final LongAdder generateCount = new LongAdder();
    private final LongAdder waitNextMillisCount = new LongAdder();
    private final LongAdder clockBackwardCount = new LongAdder();

    /**
     * 构造函数：workerId 按「JVM参数 > 环境变量 > 物理网卡MAC推导 > 随机」自动解析
     */
    public SnowflakeIdGeneratorCas() {
        this(resolveWorkerId(), resolveMaxClockBackwardMs());
    }

    /**
     * 构造函数：手动指定机器ID（0-9999），时钟回拨容忍上限取默认配置
     */
    public SnowflakeIdGeneratorCas(long workerId) {
        this(workerId, resolveMaxClockBackwardMs());
    }

    /**
     * 构造函数：手动指定机器ID与时钟回拨容忍上限
     *
     * @param workerId           机器ID，范围 [0, 9999]，分布式环境下必须全局唯一
     * @param maxClockBackwardMs 时钟回拨容忍上限（毫秒），回拨超过该值抛异常；0 表示不容忍任何回拨
     */
    public SnowflakeIdGeneratorCas(long workerId, long maxClockBackwardMs) {
        checkWorkerId(workerId);
        if (maxClockBackwardMs < 0) {
            throw new IllegalArgumentException("maxClockBackwardMs must be >= 0, but was " + maxClockBackwardMs);
        }
        this.workerId = workerId;
        this.maxClockBackwardMs = maxClockBackwardMs;
        StringBuilder segment = new StringBuilder(WORKER_ID_DIGITS);
        appendPadded(segment, workerId, WORKER_ID_DIGITS);
        this.workerIdSegment = segment.toString();
        log.info("[Snowflake] ID generator initialized: workerId={}, maxClockBackwardMs={}", workerId, maxClockBackwardMs);
    }

    /**
     * 生成下一个ID（线程安全，无锁 CAS 核心流程）
     *
     * @return 25 位纯数字ID，如 2026101216345512300770042
     * @throws SnowflakeClockBackwardException 时钟回拨超过容忍上限时抛出
     */
    public String nextId() {
        while (true) {
            // 每次循环都重新读取时钟：避免线程挂起（GC停顿/调度延迟）后使用陈旧时间快照，
            // 把「自身快照过时」误判成「时钟回拨」而错误抛出异常
            long now = System.currentTimeMillis();
            long state = lastTimestampAndSequence.get();
            long lastTimestamp = state >>> SEQUENCE_BITS;
            long lastSequence = state & SEQUENCE_MASK;

            // ======== 情况1：时钟回拨 ========
            if (now < lastTimestamp) {
                long offset = lastTimestamp - now;
                clockBackwardCount.increment();
                if (offset > maxClockBackwardMs) {
                    throw new SnowflakeClockBackwardException("Clock moved backwards by {} ms, exceeding the max tolerable offset of {} ms, refusing to generate id. Please check NTP clock synchronization",
                            offset, maxClockBackwardMs);
                }
                // 容忍范围内：等待时钟追平（多睡1ms避免边界抖动）后重新尝试
                sleepQuietly(offset + 1);
                continue;
            }

            // ======== 情况2：同一毫秒内，序列号+1 ========
            if (now == lastTimestamp) {
                long nextSequence = lastSequence + 1;
                // 该毫秒的 10000 个序列号已用完，等待进入下一毫秒
                if (nextSequence > MAX_SEQUENCE) {
                    waitNextMillis(lastTimestamp);
                    waitNextMillisCount.increment();
                    continue;
                }
                if (lastTimestampAndSequence.compareAndSet(state, (now << SEQUENCE_BITS) | nextSequence)) {
                    generateCount.increment();
                    return buildId(now, nextSequence);
                }
                // CAS 失败说明被其他线程抢先更新，重试
                continue;
            }

            // ======== 情况3：进入新的毫秒，序列号归零 ========
            if (lastTimestampAndSequence.compareAndSet(state, now << SEQUENCE_BITS)) {
                generateCount.increment();
                return buildId(now, 0);
            }
            // CAS 失败重试
        }
    }

    /**
     * 批量生成ID（逐个生成，线程安全）
     *
     * @param count 生成数量
     * @return ID数组；count &lt;= 0 时返回空数组
     */
    public String[] nextIds(int count) {
        if (count <= 0) {
            return new String[0];
        }
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = nextId();
        }
        return ids;
    }

    /**
     * 组装最终的ID字符串（时间前缀按秒缓存，毫秒/机器ID/序列号补零拼接）
     */
    private String buildId(long timestamp, long sequence) {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        sb.append(getSecondPrefix(timestamp));
        appendPadded(sb, timestamp % 1000, MILLIS_DIGITS);
        sb.append(workerIdSegment);
        appendPadded(sb, sequence, SEQUENCE_DIGITS);
        return sb.toString();
    }

    /**
     * 获取秒级时间前缀（yyyyMMddHHmmss）：同一秒内复用缓存，仅在秒变化时格式化
     */
    private String getSecondPrefix(long timestamp) {
        long second = timestamp / 1000;
        SecondPrefix cached = secondPrefixRef.get();
        if (cached != null && cached.second == second) {
            return cached.prefix;
        }
        // 竞争下可能重复格式化同一秒（幂等无危害），entry 整体原子发布保证读到的 second 与 prefix 一致
        SecondPrefix fresh = new SecondPrefix(second, formatSecond(second));
        secondPrefixRef.set(fresh);
        return fresh.prefix;
    }

    /**
     * 将 epoch 秒格式化为 yyyyMMddHHmmss（使用 JVM 默认时区）
     */
    private static String formatSecond(long second) {
        return SECOND_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochSecond(second), ZoneId.systemDefault()));
    }

    /**
     * 等待直到真实时钟超过指定的毫秒时间戳（毫秒内忙等，主动让出CPU时间片）
     */
    private void waitNextMillis(long lastTimestamp) {
        long timestamp;
        do {
            Thread.yield();
            timestamp = System.currentTimeMillis();
        } while (timestamp <= lastTimestamp);
    }

    /**
     * 睡眠指定毫秒（时钟回拨等待用），被中断时恢复中断标志并抛出异常
     */
    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LuckyRuntimeException("Thread interrupted while waiting for the clock to catch up", e);
        }
    }

    // ==================== 机器ID解析 ====================

    /**
     * 解析机器ID：JVM参数 > 环境变量 > 物理网卡MAC稳定推导 > 随机兜底
     */
    private static long resolveWorkerId() {
        String configured = System.getProperty(WORKER_ID_PROPERTY);
        String source = "JVM property -D" + WORKER_ID_PROPERTY;
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(WORKER_ID_ENV);
            source = "environment variable " + WORKER_ID_ENV;
        }
        if (configured != null && !configured.trim().isEmpty()) {
            long workerId;
            try {
                workerId = Long.parseLong(configured.trim());
            } catch (NumberFormatException e) {
                throw new LuckyRuntimeException("Invalid workerId config value [" + configured + "] (from " + source + "): not a valid number");
            }
            checkWorkerId(workerId);
            log.info("[Snowflake] workerId={} resolved from {}", workerId, source);
            return workerId;
        }
        Long macWorkerId = resolveWorkerIdByMac();
        if (macWorkerId != null) {
            log.info("[Snowflake] workerId is not explicitly configured, derived from physical NIC MAC: workerId={}", macWorkerId);
            return macWorkerId;
        }
        long random = ThreadLocalRandom.current().nextLong(MAX_WORKER_ID + 1);
        log.warn("[Snowflake] Cannot derive workerId from any physical NIC, randomly assigned workerId={}. " +
                        "Random workerId may collide in a distributed environment, please configure it explicitly via -D{}=? or environment variable {}",
                random, WORKER_ID_PROPERTY, WORKER_ID_ENV);
        return random;
    }

    /**
     * 基于物理网卡MAC推导机器ID：
     * 过滤回环/虚拟/未启用网卡，按网卡名排序后对所有MAC做哈希。
     * 排序保证同一台机器多次运行（网卡枚举顺序可能变化）推导结果稳定一致
     */
    private static Long resolveWorkerIdByMac() {
        try {
            List<NetworkInterface> candidates = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    candidates.add(ni);
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            Collections.sort(candidates, Comparator.comparing(NetworkInterface::getName));
            long hash = 1125899906842597L;
            for (NetworkInterface ni : candidates) {
                for (byte b : ni.getHardwareAddress()) {
                    hash = 31 * hash + b;
                }
            }
            long range = MAX_WORKER_ID + 1;
            return (hash % range + range) % range;
        } catch (Exception e) {
            log.warn("[Snowflake] Failed to read network interface information, unable to derive workerId automatically", e);
            return null;
        }
    }

    private static void checkWorkerId(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be in range [0, " + MAX_WORKER_ID + "], but was " + workerId);
        }
    }

    /**
     * 解析时钟回拨容忍上限：JVM参数 > 环境变量 > 默认值1000ms
     */
    private static long resolveMaxClockBackwardMs() {
        String configured = System.getProperty(MAX_BACKWARD_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(MAX_BACKWARD_ENV);
        }
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_MAX_CLOCK_BACKWARD_MS;
        }
        long value;
        try {
            value = Long.parseLong(configured.trim());
        } catch (NumberFormatException e) {
            throw new LuckyRuntimeException("Invalid maxClockBackwardMs config value [" + configured + "]: not a valid number");
        }
        if (value < 0) {
            throw new LuckyRuntimeException("Invalid maxClockBackwardMs config value [" + configured + "]: must not be negative");
        }
        return value;
    }

    // ==================== 解析方法 ====================

    /**
     * 解析ID中的生成时间（使用 JVM 默认时区）
     * <p>
     * 注意：此处故意不用 DateTimeFormatter.parse 解析，因为 Java 8 对 "yyyyMMddHHmmssSSS" 这类
     * 紧凑数字串的解析存在 JDK 版本行为差异（Java 8 会解析失败，高版本 JDK 正常），
     * 定长纯数字格式直接按下标截取解析最稳定
     */
    public LocalDateTime parseLocalDateTime(String id) {
        checkId(id);
        return LocalDateTime.of(
                Integer.parseInt(id.substring(0, 4)),                  // 年
                Integer.parseInt(id.substring(4, 6)),                  // 月
                Integer.parseInt(id.substring(6, 8)),                  // 日
                Integer.parseInt(id.substring(8, 10)),                 // 时
                Integer.parseInt(id.substring(10, 12)),                // 分
                Integer.parseInt(id.substring(12, 14)),                // 秒
                Integer.parseInt(id.substring(14, 17)) * 1_000_000     // 毫秒转纳秒
        );
    }

    /**
     * 解析ID中的生成时间戳（epoch 毫秒，使用 JVM 默认时区）
     */
    public long parseTimestamp(String id) {
        return parseLocalDateTime(id).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 解析ID中的机器ID
     */
    public long parseWorkerId(String id) {
        checkId(id);
        return Long.parseLong(id.substring(SECOND_TIME_DIGITS + MILLIS_DIGITS, SECOND_TIME_DIGITS + MILLIS_DIGITS + WORKER_ID_DIGITS));
    }

    /**
     * 解析ID中的序列号
     */
    public long parseSequence(String id) {
        checkId(id);
        return Long.parseLong(id.substring(SECOND_TIME_DIGITS + MILLIS_DIGITS + WORKER_ID_DIGITS));
    }

    private static void checkId(String id) {
        if (id == null || id.length() != ID_LENGTH) {
            throw new IllegalArgumentException("Invalid snowflake id [" + id + "], expected length is " + ID_LENGTH + " digits");
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Invalid snowflake id [" + id + "], must consist of digits only");
            }
        }
    }

    /**
     * 将数值以固定宽度补零后追加到字符串缓冲区（高位不足补 '0'）
     */
    private static void appendPadded(StringBuilder sb, long value, int width) {
        long pow = POWERS_OF_TEN[width];
        for (long d = pow / 10; d > 0; d /= 10) {
            sb.append(DIGITS[(int) ((value / d) % 10)]);
        }
    }

    /**
     * 秒级时间前缀缓存条目（second 与 prefix 一起原子发布，读取无撕裂）
     */
    private static final class SecondPrefix {
        final long second;
        final String prefix;

        SecondPrefix(long second, String prefix) {
            this.second = second;
            this.prefix = prefix;
        }
    }

    // ==================== 统计信息 ====================

    public long getGenerateCount() {
        return generateCount.sum();
    }

    public long getWaitNextMillisCount() {
        return waitNextMillisCount.sum();
    }

    public long getClockBackwardCount() {
        return clockBackwardCount.sum();
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getMaxClockBackwardMs() {
        return maxClockBackwardMs;
    }

    // ==================== 性能测试与唯一性验证 ====================

    public static void main(String[] args) throws InterruptedException {
        SnowflakeIdGeneratorCas generator = new SnowflakeIdGeneratorCas(77);

        System.out.println("ID结构：yyyyMMddHHmmss(14) + 毫秒(3) + 机器ID(4) + 序列号(4) = " + ID_LENGTH + "位");
        System.out.println("示例ID（workerId=77）：");
        for (int i = 0; i < 10; i++) {
            String id = generator.nextId();
            System.out.printf("  %s  ->  时间=%s，机器ID=%d，序列号=%d%n",
                    id, generator.parseLocalDateTime(id), generator.parseWorkerId(id), generator.parseSequence(id));
        }

        // ==================== 单线程性能测试 ====================
        int count = 1_000_000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            generator.nextId();
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("单线程：%d 个ID，耗时 %d ms，QPS：%.2f 万/秒%n",
                count, elapsed, count / (elapsed / 1000.0) / 10000);

        // ==================== 多线程并发唯一性验证（共享同一实例，最苛刻的竞争场景） ====================
        int threadCount = 8;
        int perThread = 100_000;
        Set<String> idSet = ConcurrentHashMap.newKeySet();
        Thread[] threads = new Thread[threadCount];
        start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    idSet.add(generator.nextId());
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        elapsed = System.currentTimeMillis() - start;
        long total = (long) threadCount * perThread;
        System.out.printf("多线程(%d线程共享单实例)：%d 个ID，耗时 %d ms，QPS：%.2f 万/秒%n",
                threadCount, total, elapsed, total / (elapsed / 1000.0) / 10000);
        System.out.printf("唯一性校验：生成 %d 个，去重后 %d 个，%s%n",
                total, idSet.size(), idSet.size() == total ? "无重复ID【通过】" : "出现重复ID【失败】");
        System.out.printf("统计：累计生成=%d，序列号用尽等待=%d次，时钟回拨=%d次%n",
                generator.getGenerateCount(), generator.getWaitNextMillisCount(), generator.getClockBackwardCount());
    }
}
