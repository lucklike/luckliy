package com.luckyframework.httpclient.proxy.processor;

import com.luckyframework.common.Console;
import com.luckyframework.common.FileUnitUtils;
import com.luckyframework.common.ProgressBar;
import com.luckyframework.common.StringUtils;

/**
 * 控制台打印进度监控器
 */
public class ConsolePrintProgressMonitor implements ProgressMonitor {

    private static final ProgressBar bar = ProgressBar.styleOne(50);

    @Override
    public void sniffing(Progress progress) {

        if(!progress.isStart()) {
            Console.println("Downloading {} to {}", progress.getHeaderMataData().getRequestUrl(), progress.getSavePath());
        } else {
             bar.refresh(
                    "",
                    progress.getTotal(),
                    progress.getComplete(),
                    ((Double)(progress.getAvgSpeed()/1024)).longValue() + "kb/s",
                    progress.geTakeTime()+ "s",
                    ((Double)progress.getRemainTime()).longValue() + "s"
                    );
        }
        if (progress.isEnd()) {
            Console.print("\rDownload successful，take time {}s\n", progress.getTotalTime());
        }
    }
}
