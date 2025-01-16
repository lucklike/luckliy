package com.luckyframework.io;

/**
 * 存在存储介质的流
 */
public interface StorageMediumStream {

    /**
     * 删除储存介质
     *
     * @return 是否已经成功删除
     */
    boolean deleteStorageMedium();

    /**
     * 存储介质的大小
     *
     * @return 存储介质的大小
     */
    long length();
}
