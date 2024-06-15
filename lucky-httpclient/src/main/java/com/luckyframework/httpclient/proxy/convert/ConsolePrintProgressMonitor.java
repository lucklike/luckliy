package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.Console;
import com.luckyframework.common.ProgressBar;
import com.luckyframework.common.UnitUtils;

/**
 * 控制台打印进度监控器
 */
public class ConsolePrintProgressMonitor implements ProgressMonitor {

    private static final ProgressBar bar = ProgressBar.styleOne(50);
    private static final String BLANK_STR = getBlankStr();

    @Override
    public void beforeBeginning(Progress progress) {
        Console.println("Downloading {} to {}", progress.getHeaderMataData().getRequestUrl(), progress.getSavePath());
    }

    @Override
    public void sniffing(Progress progress) {
        System.out.print("\r" + BLANK_STR);
        bar.refresh("", progress.getCompleteRate(), UnitUtils.byteTo(progress.getTotal()), UnitUtils.byteTo(progress.getComplete()), UnitUtils.byteTo(((Double) (progress.getAvgSpeed())).longValue()) + "/s", UnitUtils.secToTime(progress.geTakeTime()), UnitUtils.secToTime(((Double) progress.getRemainTime()).longValue()));

    }

    @Override
    public void afterCompleted(Progress progress) {
        System.out.print("\r" + BLANK_STR);
        Console.print("\rDownload successful，take time {}\n", UnitUtils.secToTime(progress.getTotalTime()));
    }

    private static String getBlankStr() {
        int len = 150;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
