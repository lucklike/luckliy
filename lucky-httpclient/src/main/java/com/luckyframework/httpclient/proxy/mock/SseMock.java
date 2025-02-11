package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.httpclient.proxy.sse.standard.SseConstant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SSE响应数据模拟
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/09/02 10:30
 */
public class SseMock {

    /**
     * 数据段集合
     */
    private final List<Section> sections = new ArrayList<>();

    /**
     * 资源路径
     */
    private String resourceLocation;

    /**
     * 创建一个SSE响应数据模拟
     *
     * @return SSE响应数据模拟
     */
    public static SseMock create() {
        return new SseMock();
    }

    /**
     * 从某个资源中加载SSE响应模拟数据
     *
     * @param resourceLocation 资源路径
     * @return SSE响应数据模拟
     */
    public static SseMock resource(String resourceLocation) {
        SseMock sseMock = new SseMock();
        sseMock.resourceLocation = resourceLocation;
        return sseMock;
    }

    /**
     * 生成一个数据段
     *
     * @return 数据段
     */
    public static Section section() {
        return new Section();
    }

    /**
     * 添加一个数据段
     *
     * @param section 数据段
     * @return this
     */
    public SseMock addSection(Section section) {
        sections.add(section);
        return this;
    }

    /**
     * 添加一个只包含<b>data</b>数据行的数据段
     *
     * @param data data数据行内容
     * @return this
     */
    public SseMock addData(String data) {
        return addSection(section().data(data));
    }

    /**
     * 获取文本流
     *
     * @return 文本流
     */
    public InputStream getTxtStream() {
        if (resourceLocation != null) {
            try {
                return CommonFunctions.resource(resourceLocation).getInputStream();
            } catch (IOException e) {
                throw new LuckyIOException(e);
            }
        }

        return new ByteArrayInputStream(getData().getBytes());
    }

    /**
     * 将数据段转化为字符串
     *
     * @return 数据段字符串
     */
    public String getData() {
        StringBuilder data = new StringBuilder();
        for (Section section : sections) {
            data.append(section.getSection()).append("\n");
        }
        return data.toString();
    }

    @Override
    public String toString() {
        return getData();
    }

    /**
     * 数据段
     */
    public static class Section {

        /**
         * 数据行集合
         */
        private final List<Line> lines = new ArrayList<>();

        /**
         * 添加一个数据行
         *
         * @param line 数据行
         * @return this
         */
        public Section line(Line line) {
            lines.add(line);
            return this;
        }

        /**
         * 添加一个数据行
         *
         * @param key   数据行key
         * @param value 数据行value
         * @return this
         */
        public Section line(String key, String value) {
            return line(Line.of(key, value));
        }

        /**
         * 添加一个ID数据行
         *
         * @param value 数据行value
         * @return this
         */
        public Section id(String value) {
            return line(SseConstant.ID, value);
        }

        /**
         * 添加一个EVENT数据行
         *
         * @param value 数据行value
         * @return this
         */
        public Section event(String value) {
            return line(SseConstant.EVENT, value);
        }

        /**
         * 添加一个DATA数据行
         *
         * @param value 数据行value
         * @return this
         */
        public Section data(String value) {
            return line(SseConstant.DATA, value);
        }

        /**
         * 添加一个RETRY数据行
         *
         * @param value 数据行value
         * @return this
         */
        public Section retry(String value) {
            return line(SseConstant.RETRY, value);
        }

        /**
         * 添加一个COMMENT数据行
         *
         * @param value 数据行value
         * @return this
         */
        public Section comment(String value) {
            return line(SseConstant.COMMENT, value);
        }

        /**
         * 将数据行集合转为字符串
         *
         * @return 数据行字符串
         */
        public String getSection() {
            StringBuilder sb = new StringBuilder();
            for (Line line : lines) {
                sb.append(line.getLine()).append("\n");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return getSection();
        }
    }

    /**
     * 数据行
     */
    public static class Line {

        /**
         * KEY
         */
        private final String key;

        /**
         * VALUE
         */
        private final String value;

        /**
         * 数据行构造器
         *
         * @param key   KEY
         * @param value VALUE
         */
        private Line(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * 生成一个数据行实例
         *
         * @param key   KEY
         * @param value VALUE
         * @return 数据行实例
         */
        public static Line of(String key, String value) {
            return new Line(key, value);
        }

        /**
         * 将数据行转化为字符串
         *
         * @return 数据行字符串
         */
        public String getLine() {
            return key + ": " + value;
        }

        @Override
        public String toString() {
            return getLine();
        }
    }
}
