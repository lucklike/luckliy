package com.luckyframework.httpclient.generalapi;

/**
 * 文件说明
 */
public class FileDesc {

    private final String fileName;
    private final long size;


    public FileDesc(String fileName, long size) {
        this.fileName = fileName;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "FileDesc{" +
                "fileName='" + fileName + '\'' +
                ", size=" + size +
                '}';
    }
}
