package com.luckyframework.common;

public class ProgressBar {

    /**
     * 长度
     */
    private int length;

    /**
     * 左边界
     */
    private String leftBorder;

    /**
     * 右边界
     */
    private String rightBorder;

    /**
     * 填充符号
     */
    private String fill;

    /**
     * 空白符号
     */
    private String blank;

    /**
     * 标志符号
     */
    private String mark;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getRightBorder() {
        return rightBorder;
    }

    public void setRightBorder(String rightBorder) {
        this.rightBorder = rightBorder;
    }

    public String getFill() {
        return fill;
    }

    public void setFill(String fill) {
        this.fill = fill;
    }

    public String getBlank() {
        return blank;
    }

    public void setBlank(String blank) {
        this.blank = blank;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getLeftBorder() {
        return leftBorder;
    }

    public void setLeftBorder(String leftBorder) {
        this.leftBorder = leftBorder;
    }

    /**
     * 任务名
     */
    private String taskName;

    /**
     * 进度信息
     */
    private String progress;

    /**
     * 速度信息
     */
    private String speed;

    /**
     * 耗时信息
     */
    private String elapsedTime;

    /**
     * 剩余时间信息
     */
    private String remainTime;

    private ProgressBar() {

    }

    /**
     * <pre>
     * 样式一
     * [/]🁢🁢🁢🁢🁢🁢🁢🁢🁢🁢🁢🁢🁢🁢🁢🁢              🏁 10/100 (10%) | 100kb/s | 20s | 50s
     * </pre>
     */
    public static ProgressBar styleOne(int length) {
        ProgressBar bar = new ProgressBar();
        bar.setLength(length);
        bar.setLeftBorder("");
        bar.setFill("🁢");
        bar.setBlank(" ");
        bar.setMark("🁢");
        bar.setRightBorder("🏁 ");
        return bar;
    }

    public String getBar(String taskName, double rate, String total, String complete, String speed, String elapsedTime, String remainTime) {
        int fillLen = (int)(length * rate);
        String[] array = {"-", "/", "\\"};
        String s = "[" + array[(int) (3 * Math.random())] + "]";
        String d = "[" + array[(int) (3 * Math.random())] + "]";
        String[] markArr = {mark, "🁣"};
        String _mark = markArr[(int) (2 * Math.random())];
        if (fillLen == length) {
            s = "";
            _mark = fill;
            d = "✅";
        }

        StringBuilder sb = new StringBuilder(getStr(taskName)).append(s).append(leftBorder);
        for (int i = 0; i < length; i++) {
            if (i < fillLen) {
                sb.append(fill);
            } else if (i == fillLen){
                sb.append(_mark);
            } else {
                sb.append(blank);
            }
        }

        sb.append(d).append(" ")
                .append(complete)
                .append("/")
                .append(total)
                .append(" | ")
                .append(StringUtils.decimalToPercent(rate, 3, 2));
        if (StringUtils.hasText(speed)) {
            sb.append(" | ").append(speed);
        }
        if (StringUtils.hasText(elapsedTime)) {
            sb.append(" | ").append(elapsedTime);
        }
        if (StringUtils.hasText(remainTime)) {
            sb.append(" | ").append(remainTime);
        }

        return sb.toString();
    }

    public void refresh(String taskName, double rate, String total, String complete, String speed, String elapsedTime, String remainTime) {
        System.out.print("\r" + getBar(
                taskName,
                rate,
                total,
                complete,
                speed,
                elapsedTime,
                remainTime
        ));
    }

    private String getStr(String str) {
        return StringUtils.hasText(str) ? str : "";
    }
}
