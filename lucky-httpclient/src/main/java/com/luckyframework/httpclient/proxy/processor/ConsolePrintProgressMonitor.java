package com.luckyframework.httpclient.proxy.processor;

import com.luckyframework.common.Console;
import com.luckyframework.common.StringUtils;

/**
 * 控制台打印进度监控器
 */
public class ConsolePrintProgressMonitor implements ProgressMonitor {
    @Override
    public void sniffing(Progress progress) {

        if(!progress.isStart()) {
            Console.println("Download {} to {}", progress.getHeaderMataData().getRequestUrl(), progress.getSavePath());
        } else {
            Console.print(
                "\rDownload progress: {}/{} ({}), average download speed: {}b/s, elapsed time: {}s, remaining time: {}s",
                    progress.getComplete(),
                    progress.getTotal(),
                    StringUtils.decimalToPercent(progress.getCompleteRate()),
                    progress.getAvgSpeed(),
                    progress.geTakeTime(),
                    ((Double)progress.getRemainTime()).longValue()
            );
        }
        if (progress.isEnd()) {
            Console.println("\nDownload successful，take time {}s", progress.getTotalTime());
        }
    }
}
