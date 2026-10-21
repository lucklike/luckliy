package com.luckyframework.httpclient.generalapi.chunk;

/**
 * 分片常量。
 * <pre>
 *     分片大小的单一事实来源：上传侧（{@link FileChunkHandle}）与下载侧
 *     （{@code RangeDownloadApi}/{@code @RangeDownload}）都引用本常量，
 *     调整默认分片大小时只需改这里，不必两侧各改一份。
 *
 *     本类位于分片包且不含任何下载逻辑，因此依赖方向为下载依赖分片，
 *     分片工具不再反向依赖具体的下载API。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-21
 * @see FileChunkHandle#of(java.io.File)
 */
public class ChunkConstants {

    /**
     * 默认分片大小：5M。
     * <pre>
     *     取5M是为了在「单次请求体积」与「请求次数」之间取得折中：
     *     分片过小会放大HTTP请求次数与服务端合并成本，过大则单次失败重传的代价更高，
     *     同时分片大小还决定{@link FileChunk#getContent()}整片读入内存时的占用。
     * </pre>
     */
    public static final long DEFAULT_CHUNK_SIZE = 1024 * 1024 * 5;

    /**
     * 允许的最大分片大小：Integer.MAX_VALUE字节（约2G）。
     * <pre>
     *     分片内容最终要用{@code byte[]}承载，数组下标是int，
     *     超过该值会在读取分片时抛出{@link NegativeArraySizeException}一类的误导性异常，
     *     故在切分入口即{@link ChunkUtils#shard(long, long)}处拦截。
     * </pre>
     */
    public static final long MAX_CHUNK_SIZE = Integer.MAX_VALUE;
}
