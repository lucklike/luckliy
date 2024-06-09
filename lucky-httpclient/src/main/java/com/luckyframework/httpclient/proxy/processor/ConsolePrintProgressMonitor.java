package com.luckyframework.httpclient.proxy.processor;

import com.luckyframework.common.Console;
import com.luckyframework.common.ProgressBar;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.UnitUtils;

/**
 * 控制台打印进度监控器
 */
public class ConsolePrintProgressMonitor implements ProgressMonitor {

    private static final ProgressBar bar = ProgressBar.styleOne(50);
    private static final String BLANK_STR = getBlankStr(150);

    @Override
    public void sniffing(Progress progress) {

        if (!progress.isStart()) {
            Console.println("Downloading {} to {}", progress.getHeaderMataData().getRequestUrl(), progress.getSavePath());
        } else {
            System.out.print("\r" + BLANK_STR);
            bar.refresh(
                    "",
                    progress.getCompleteRate(),
                    UnitUtils.byteTo(progress.getTotal()),
                    UnitUtils.byteTo(progress.getComplete()),
                    UnitUtils.byteTo(((Double) (progress.getAvgSpeed())).longValue()) + "/s",
                    UnitUtils.secToTime(progress.geTakeTime()),
                    UnitUtils.secToTime(((Double) progress.getRemainTime()).longValue())
            );
        }
        if (progress.isEnd()) {
            System.out.print("\r" + BLANK_STR);
            Console.print("\rDownload successful，take time {}\n", UnitUtils.secToTime(progress.getTotalTime()));
        }
    }

    private static String getBlankStr(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
