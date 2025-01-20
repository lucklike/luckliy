package com.luckyframework.httpclient.generalapi.shard;

/**
 * 分片信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 03:34
 */
public class Shard {

    /**
     * 索引信息
     */
    private long index;

    /**
     * 读取开始位置
     */
    private long start;

    /**
     * 读取结束为止
     */
    private long end;

    public long getSize() {
        return end - start;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "ShardInfo{" +
                "index=" + index +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
