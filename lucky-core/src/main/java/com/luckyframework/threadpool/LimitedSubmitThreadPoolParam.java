package com.luckyframework.threadpool;

/**
 * 用于创建{@link LimitedSubmitThreadPoolExecutor}线程池的惨谁
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 11:33
 */
public class LimitedSubmitThreadPoolParam extends ThreadPoolParam{

    /** 提交限制，当队列中的任务达到该限制时会对'提交操作'进行限制*/
    protected int limitedSubmitCount = 100;
    /** 提交等待时间，当队列中的任务达到限制时，提交操作将进入等待状态，该值为等待时长*/
    protected long submitWaitTime = 1000L;
    /** 提示信息*/
    protected String submitWaitPromptName = "limitedSubmitThreadPool";

    public int getLimitedSubmitCount() {
        return limitedSubmitCount;
    }

    public void setLimitedSubmitCount(int limitedSubmitCount) {
        this.limitedSubmitCount = limitedSubmitCount;
    }

    public long getSubmitWaitTime() {
        return submitWaitTime;
    }

    public void setSubmitWaitTime(long submitWaitTime) {
        this.submitWaitTime = submitWaitTime;
    }

    public String getSubmitWaitPromptName() {
        return submitWaitPromptName;
    }

    public void setSubmitWaitPromptName(String submitWaitPromptName) {
        this.submitWaitPromptName = submitWaitPromptName;
    }

    @Override
    public String toString() {
        return "LimitedSubmitThreadPoolParam{" +
                "limitedSubmitCount=" + limitedSubmitCount +
                ", submitWaitTime=" + submitWaitTime +
                ", submitWaitPromptName='" + submitWaitPromptName + '\'' +
                ", corePoolSize=" + corePoolSize +
                ", maximumPoolSize=" + maximumPoolSize +
                ", blockingQueueSize=" + blockingQueueSize +
                ", keepAliveTime=" + keepAliveTime +
                ", nameFormat='" + nameFormat + '\'' +
                ", blockingQueueFactory=" + blockingQueueFactory +
                ", rejectedExecutionHandlerFactory=" + rejectedExecutionHandlerFactory +
                '}';
    }
}
