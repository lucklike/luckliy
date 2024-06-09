package com.luckyframework.httpclient.proxy.processor;

import com.luckyframework.common.Console;
import com.luckyframework.common.ProgressBar;
import com.luckyframework.common.UnitUtils;

/**
 * 控制台打印进度监控器
 */
public class ConsolePrintProgressMonitor implements ProgressMonitor {

    private static final ProgressBar bar = ProgressBar.styleOne(50);

    @Override
    public void sniffing(Progress progress) {

        if (!progress.isStart()) {
            Console.println("Downloading {} to {}", progress.getHeaderMataData().getRequestUrl(), progress.getSavePath());
        } else {
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
            Console.print("\rDownload successful，take time {}\n", UnitUtils.secToTime(progress.getTotalTime()));
        }
    }
}
