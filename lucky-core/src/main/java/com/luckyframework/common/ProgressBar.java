package com.luckyframework.common;

public class ProgressBar {


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
     * [==================>                 ]
     * </pre>
     */
    public static ProgressBar styleOne() {
        ProgressBar bar = new ProgressBar();
        bar.setLeftBorder("[");
        bar.setFill("=");
        bar.setBlank(" ");
        bar.setMark(">");
        bar.setRightBorder("]");
        return bar;
    }
}
