package com.luckyframework.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class FileUtils {


    /**
     * 关闭Closeable对象忽略异常
     *
     * @param closeable Closeable对象
     */
    public static void closeIgnoreException(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    /**
     * 创建保存文件的文件夹
     *
     * @param saveFolder 保存文件的文件夹
     */
    public static void createSaveFolder(File saveFolder) {
        if (saveFolder.isFile()) {
            throw new IllegalArgumentException("The destination of the copy must be a folder with the wrong path: " + saveFolder.getAbsolutePath());
        }
        if (!saveFolder.exists()) {
            saveFolder.mkdirs();
        }
    }
}
