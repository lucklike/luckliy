package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.io.LineEventListener;

/**
 * 遇到换行符时随机休眠一段时间
 */
public class RandomSleepLineEventListener implements LineEventListener {

    private final int minSleep;
    private final int maxSleep;

    public RandomSleepLineEventListener(int minSleep, int maxSleep) {
        this.minSleep = minSleep;
        this.maxSleep = maxSleep;
    }

    public RandomSleepLineEventListener() {
        this(20, 500);
    }


    @Override
    public void onNewline(String line, int lineNumber) {
        try {
            Thread.sleep(CommonFunctions.random(minSleep, maxSleep));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
