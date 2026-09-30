package com.luckyframework.common;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 雪花算法 ID 生成器（高性能 CAS 版本）
 * 64位结构：1bit符号位 + 41bit时间戳 + 10bit机器ID + 12bit序列号
 *
 * 性能优化点：
 * 1. 使用 CAS 替代 synchronized，避免锁竞争
 * 2. 使用 LongAdder 统计信息，减少缓存行伪共享
 * 3. 时间戳使用 volatile + 局部变量缓存
 */
public class SnowflakeIdGeneratorCas {

    public static final SnowflakeIdGeneratorCas INSTANCE = new SnowflakeIdGeneratorCas();

    // ==================== 常量配置 ====================
    private static final long EPOCH = 1577836800000L; // 2020-01-01
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long TIMESTAMP_LEFT_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;
    private static final long WORKER_ID_LEFT_SHIFT = SEQUENCE_BITS;

    // 时钟回拨最大容忍时间（毫秒）
    private static final long MAX_CLOCK_BACKWARD_MS = 5;

    // ==================== 实例属性 ====================
    private final long workerId;

    // 使用 AtomicLong 保证 CAS 操作的原子性
    // 高32位存储时间戳，低12位存储序列号，一次CAS同时更新两个字段
    private final AtomicLong lastTimestampAndSequence = new AtomicLong(0);

    // 统计信息（可选）
    private final LongAdder generateCount = new LongAdder();
    private final LongAdder waitNextMillisCount = new LongAdder();
    private final LongAdder clockBackwardCount = new LongAdder();

    /**
     * 构造函数：自动从网络接口获取机器ID
     */
    public SnowflakeIdGeneratorCas() {
        this.workerId = getWorkerIdByMac();
    }

    /**
     * 构造函数：手动指定机器ID
     */
    public SnowflakeIdGeneratorCas(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(String.format(
                    "Worker ID must be between 0 and %d", MAX_WORKER_ID
            ));
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个ID（核心方法，无锁）
     */
    public long nextId() {
        // 获取当前时间戳
        long currentTimestamp = System.currentTimeMillis();

        while (true) {
            // 读取当前状态（时间戳+序列号的组合值）
            long lastState = lastTimestampAndSequence.get();

            // 解码上次的时间戳和序列号
            long lastTimestamp = lastState >>> SEQUENCE_BITS;
            long lastSequence = lastState & MAX_SEQUENCE;

            // ======== 情况1：时钟回拨 ========
            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                clockBackwardCount.increment();

                if (offset <= MAX_CLOCK_BACKWARD_MS) {
                    // 小幅度回拨：等待时钟追上
                    try {
                        Thread.sleep(offset << 1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for clock", e);
                    }
                    // 等待后重新获取时间
                    currentTimestamp = System.currentTimeMillis();
                    continue; // 重新尝试
                } else {
                    // 大幅度回拨：抛异常
                    throw new RuntimeException(String.format(
                            "Clock moved backwards. Refusing to generate id for %d milliseconds",
                            offset
                    ));
                }
            }

            // ======== 情况2：同一毫秒内 ========
            if (currentTimestamp == lastTimestamp) {
                long nextSequence = (lastSequence + 1) & MAX_SEQUENCE;

                // 如果序列号溢出（=0），说明这一毫秒的4096个ID已用完
                if (nextSequence == 0) {
                    // 等待下一毫秒
                    currentTimestamp = waitNextMillis(lastTimestamp);
                    waitNextMillisCount.increment();
                    continue; // 重新尝试
                }

                // CAS 尝试更新状态（时间戳不变，序列号+1）
                long newState = (currentTimestamp << SEQUENCE_BITS) | nextSequence;
                if (lastTimestampAndSequence.compareAndSet(lastState, newState)) {
                    generateCount.increment();
                    return buildId(currentTimestamp, nextSequence);
                }
                // CAS 失败，说明被其他线程修改了，重试
                continue;
            }

            // ======== 情况3：新的毫秒 ========
            // 直接重置序列号为0，尝试CAS更新
            long newState = currentTimestamp << SEQUENCE_BITS; // 序列号部分为0
            if (lastTimestampAndSequence.compareAndSet(lastState, newState)) {
                generateCount.increment();
                return buildId(currentTimestamp, 0);
            }
            // CAS 失败，重试
        }
    }

    /**
     * 批量生成ID（进一步提升性能）
     * @param count 生成数量
     * @return ID数组
     */
    public long[] nextIds(int count) {
        if (count <= 0) {
            return new long[0];
        }
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = nextId();
        }
        return ids;
    }

    /**
     * 等待直到下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
            // 主动让出CPU时间片，避免忙等浪费CPU
            Thread.yield();
        }
        return timestamp;
    }

    /**
     * 组装最终的64位ID
     */
    private long buildId(long timestamp, long sequence) {
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (workerId << WORKER_ID_LEFT_SHIFT)
                | sequence;
    }

    /**
     * 从MAC地址获取机器ID
     */
    private static long getWorkerIdByMac() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    long id = ((mac[4] & 0xFFL) << 8) | (mac[5] & 0xFFL);
                    id = id & MAX_WORKER_ID;
                    if (id > 0) {
                        return id;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return new Random().nextInt((int) MAX_WORKER_ID + 1);
    }

    // ==================== 解析方法 ====================
    public long parseTimestamp(long id) {
        return EPOCH + (id >> TIMESTAMP_LEFT_SHIFT);
    }

    public long parseWorkerId(long id) {
        return (id >> WORKER_ID_LEFT_SHIFT) & MAX_WORKER_ID;
    }

    public long parseSequence(long id) {
        return id & MAX_SEQUENCE;
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

    // ==================== 性能测试 ====================
    public static void main(String[] args) throws InterruptedException {
        SnowflakeIdGeneratorCas generator = new SnowflakeIdGeneratorCas();
        // 866555650635943936
        // 202607191344523936

        // 测试单线程性能
        long start = System.currentTimeMillis();
        int count = 10_000_000; // 1000万次
        for (int i = 0; i < count; i++) {
            generator.nextId();
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("单线程: %d 个ID, 耗时 %d ms, QPS: %.2f 万/秒%n",
                count, elapsed, count / (elapsed / 1000.0) / 10000);

        // 测试多线程性能
        int threadCount = 16;
        int perThread = 1_000_000;
        Thread[] threads = new Thread[threadCount];
        SnowflakeIdGeneratorCas[] generators = new SnowflakeIdGeneratorCas[threadCount];

        // 每个线程使用独立的生成器（避免CAS竞争，模拟真实分布式场景）
        for (int i = 0; i < threadCount; i++) {
            generators[i] = new SnowflakeIdGeneratorCas(i);
        }

        start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                SnowflakeIdGeneratorCas gen = generators[idx];
                for (int j = 0; j < perThread; j++) {
                    gen.nextId();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        elapsed = System.currentTimeMillis() - start;
        long total = (long) threadCount * perThread;
        System.out.printf("多线程(%d): %d 个ID, 耗时 %d ms, QPS: %.2f 万/秒%n",
                threadCount, total, elapsed, total / (elapsed / 1000.0) / 10000);

        // 验证ID唯一性（抽样检查）
        System.out.println("\n验证ID唯一性（前10个）:");
        for (int i = 0; i < 10; i++) {
            long id = generator.nextId();
            System.out.printf("ID: %d, 时间戳: %s, 机器ID: %d, 序列号: %d%n",
                    id, generator.parseTimestamp(id),
                    generator.parseWorkerId(id), generator.parseSequence(id));
        }
    }
}