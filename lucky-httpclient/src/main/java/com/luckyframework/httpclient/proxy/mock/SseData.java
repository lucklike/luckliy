package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.httpclient.proxy.sse.SseConstant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SseData {

    private final List<Section> sections = new ArrayList<>();
    private String resourceLocation;

    public static SseData create() {
        return new SseData();
    }

    public static SseData resource(String resourceLocation) {
        SseData sseData = new SseData();
        sseData.resourceLocation = resourceLocation;
        return sseData;
    }

    public static Section section() {
        return new Section();
    }

    public SseData addSection(Section section) {
        sections.add(section);
        return this;
    }

    public SseData addData(String data) {
        return addSection(section().data(data));
    }

    public String getData() {
        StringBuilder data = new StringBuilder();
        for (Section section : sections) {
            data.append(section.getSection()).append("\n");
        }
        return data.toString();
    }

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

    public static class Section {

        private final List<Line> lines = new ArrayList<>();

        public Section line(Line line) {
            lines.add(line);
            return this;
        }

        public Section line(String key, String value) {
            return line(Line.of(key, value));
        }


        public Section id(String value) {
            return line(SseConstant.ID, value);
        }

        public Section event(String value) {
            return line(SseConstant.EVENT, value);
        }

        public Section data(String value) {
            return line(SseConstant.DATA, value);
        }


        public Section retry(String value) {
            return line(SseConstant.RETRY, value);
        }

        public Section comment(String value) {
            return line(SseConstant.COMMENT, value);
        }

        public String getSection() {
            StringBuilder sb = new StringBuilder();
            for (Line line : lines) {
                sb.append(line.getLine()).append("\n");
            }
            return sb.toString();
        }
    }

    public static class Line {
        private final String key;
        private final String value;

        private Line(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public static Line of(String key, String value) {
            return new Line(key, value);
        }

        public String getLine() {
            return key + ": " + value;
        }
    }
}
