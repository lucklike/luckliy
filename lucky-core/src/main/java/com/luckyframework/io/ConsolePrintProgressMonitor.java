package com.luckyframework.io;

import com.luckyframework.common.ProgressBar;
import com.luckyframework.common.UnitUtils;

/**
 * 控制台打印进度监控器
 */
public class ConsolePrintProgressMonitor implements ProgressMonitor {

    private static final ProgressBar bar = ProgressBar.styleOne(50);
    private static final String BLANK_STR = getBlankStr();
    private static final String _R_;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows：使用 \r + 空格覆盖
            _R_ =  "\r";
        } else {
            // Mac/Linux：使用ANSI转义
            _R_ = "\033[2K\r";
        }
    }

    @Override
    public void beforeBeginning(Progress progress) {
        System.out.printf("Download %s ", progress.getFile().getAbsolutePath());
    }

    @Override
    public void sniffing(Progress progress) {
        System.out.print(_R_ + BLANK_STR);
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

    @Override
    public void afterCompleted(Progress progress) {
        System.out.print(_R_ + BLANK_STR);
        System.out.printf(_R_ + "Download successful，take time %s\n", UnitUtils.secToTime(progress.getTotalTime()));
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
