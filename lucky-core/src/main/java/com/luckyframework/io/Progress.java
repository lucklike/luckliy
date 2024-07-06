package com.luckyframework.io;


import java.io.File;

/**
 * 下载进度信息包装类
 */
public class Progress {

    /**
     * 总字节数
     */
    private final long total;

    /**
     * 文件实例
     */
    private final File file;

    /**
     * 已完成字节数
     */
    private long complete;

    /**
     * 是否已经开始
     */
    private boolean start;

    /***
     * 是否已经结束
     */
    private boolean end;

    /**
     * 开始时间
     */
    private long startTime;

    /**
     * 结束时间
     */
    private long endTime;

    /**
     * 记录时间
     */
    private long recordTime;

    /**
     * 进度构造器
     *
     * @param file  文件实例
     * @param total 总字节数
     */
    Progress(File file, long total) {
        this.total = total;
        this.file = file;
    }


    /**
     * 开始下载
     */
    void start() {
        if (!start) {
            start = true;
            startTime = System.currentTimeMillis();
        }
    }

    /**
     * 记录完成的字节数
     *
     * @param complete 完成的字节数
     */
    void complete(long complete) {
        this.complete += complete;
        this.recordTime = System.currentTimeMillis();
    }

    /**
     * 结束下载
     */
    void end() {
        if (!end) {
            end = true;
            endTime = System.currentTimeMillis();
        }
    }

    /**
     * 任务是否已经开始
     *
     * @return 任务是否已经开始
     */
    public boolean isStart() {
        return start;
    }

    /**
     * 任务是否已经结束
     *
     * @return 任务是否已经结束
     */
    public boolean isEnd() {
        return end;
    }

    /**
     * 获取总字节数
     *
     * @return 总字节数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 获取下载完成的字节数
     *
     * @return 下载完成的字节数
     */
    public long getComplete() {
        return complete;
    }

    /**
     * 获取开始时间
     *
     * @return 开始时间
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 获取结束时间
     *
     * @return 结束时间
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * 获取下载完成的比例
     *
     * @return 下载完成的比例
     */
    public double getCompleteRate() {
        if (total <= 0) {
            return 0;
        }
        return complete * 1.0F / total;
    }

    /**
     * 获取平均下载速度（单位： byte/s）
     *
     * @return 获取平均下载速度（单位：byte/s）
     */
    public double getAvgSpeed() {
        if (complete <= 0) {
            return 0;
        }
        return complete * 1000.0F / (recordTime - startTime);
    }

    /**
     * 获取已用时（单位：秒）
     *
     * @return 已用时（单位：秒）
     */
    public long geTakeTime() {
        return (recordTime - startTime) / 1000;
    }

    /**
     * 获取剩余时间（单位：秒）
     *
     * @return 剩余时间（单位：秒）
     */
    public double getRemainTime() {
        return (total - complete) / getAvgSpeed();
    }

    /**
     * 获取下载总时间（单位：秒）
     *
     * @return 下载总时间（单位：秒）
     */
    public long getTotalTime() {
        if (isStart() && isEnd()) {
            return (recordTime - startTime) / 1000;
        }
        return -1;
    }

    /**
     * 获取文件对象
     * @return 文件对象
     */
    public File getFile() {
        return file;
    }
}
