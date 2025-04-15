package com.luckyframework.httpclient.generalapi.plugin;


import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;

import java.util.Date;

public class TimeStatisticsInfo {

    /**
     * 开始时间
     */
    private Date start;

    /**
     * 结束时间
     */
    private Date end;

    /**
     * 耗时
     */
    private long timeConsuming = -1L;

    private boolean warn = false;

    private boolean slow = false;

    /**
     * 运行过程中产生的异常
     */
    private Throwable th;

    /**
     * 执行元数据
     */
    private ExecuteMeta executeMeta;


    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Throwable getTh() {
        return th;
    }

    public void setTh(Throwable th) {
        this.th = th;
    }

    public ExecuteMeta getExecuteMeta() {
        return executeMeta;
    }

    public void setExecuteMeta(ExecuteMeta executeMeta) {
        this.executeMeta = executeMeta;
    }

    public boolean isWarn() {
        return warn;
    }

    public boolean isSlow() {
        return slow;
    }

    public boolean hasException() {
        return th != null;
    }

    public synchronized long getTimeConsuming() {
        if (timeConsuming == -1L) {
            timeConsuming = end.getTime() - start.getTime();
        }
        return timeConsuming;
    }

    public void initTag(long warn, long slow) {
        long consuming = getTimeConsuming();
        if (warn != -1L && consuming >= warn) {
            this.warn = true;
        }
        if (slow != -1L && consuming >= slow) {
            this.slow = true;
        }
    }
}
