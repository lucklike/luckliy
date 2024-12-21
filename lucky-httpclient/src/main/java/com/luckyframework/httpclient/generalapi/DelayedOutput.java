package com.luckyframework.httpclient.generalapi;

import com.luckyframework.common.Color;
import com.luckyframework.common.Console;

import static com.luckyframework.common.Color.MULBERRY;

/**
 * 延迟输出工具类
 */
public class DelayedOutput {

    private static final ThreadLocal<Integer> outputLength = new ThreadLocal<>();

    public static void setOutputLength(int length) {
        outputLength.set(length);
    }

    public static Integer getOutputLength() {
        Integer length = outputLength.get();
        return length == null ? 0 : length;
    }

    public static void clearOutputLength() {
        outputLength.remove();
    }

    public static void output(String output, Color color) {
        output(output, color, 70, 20);
    }

    public static void output(String output) {
        output(output, MULBERRY, 70, 20);
    }

    public static void output(String output, Color color, int maxLength, int delayTime)  {
        int length = output.length();
        int outputLength = getOutputLength();
        int rem = outputLength % maxLength;
        setOutputLength(outputLength + output.length());

        int j = maxLength - rem;
        while (j <= length) {
            output = output.substring(0, j) + "\n" + output.substring(j);
            length++;
            j += maxLength;
        }

        for (char c : output.toCharArray()) {
            Console.printColor(c, color);
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
