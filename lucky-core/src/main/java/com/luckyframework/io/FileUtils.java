package com.luckyframework.io;

import com.luckyframework.common.TimeUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class FileUtils {

    /**
     * 系统临时文件夹
     */
    public static final String OS_TEMP_DIR = System.getProperty("java.io.tmpdir");

    /**
     * 获取Lucky临时文件
     *
     * @return lucky临时文件
     */
    public static String getLuckyTempDir() {
        return new File(OS_TEMP_DIR, "Lucky").getAbsolutePath();
    }

    /**
     * 获取Lucky临时文件
     *
     * @param dir 文件夹路径
     * @return lucky临时文件
     */
    public static String getLuckyTempDir(String dir) {
        return new File(new File(getLuckyTempDir(), dir), TimeUtils.formatYyyyMMdd()).getAbsolutePath();
    }

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
