package com.luckyframework.io;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TimeUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static final String SOURCE_NAME_PLACEHOLDER = "{_name_}";
    public static final String SOURCE_EXTEND_PLACEHOLDER = "{.ext}";

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
     * 获取文件名，将文件名中的占位符替换为真实的文件信息
     *
     * @param filenameTemp   文件名模板
     * @param sourceFileName 原始文件名信息
     * @return 文件名
     */
    public static String getFileName(String filenameTemp, String sourceFileName) {

        // 文件模版为null或空字符时返回原文件名
        if (!StringUtils.hasText(filenameTemp)) {
            return sourceFileName;
        }

        // 解析文件名和扩展名
        String name = StringUtils.stripFilenameExtension(sourceFileName);
        String ext = StringUtils.getFilenameExtension(sourceFileName);
        ext = ext == null ? "" : "." + ext.toLowerCase();

        // 替换占位符
        String realFileName = filenameTemp
                .replace(SOURCE_NAME_PLACEHOLDER, name)
                .replace(SOURCE_EXTEND_PLACEHOLDER, ext);

        // 判断解析后的文件名是否存在扩展名，如果不存在则补上原文件名的扩展名
        String realFileExt = StringUtils.getFilenameExtension(sourceFileName);
        if (StringUtils.hasText(realFileExt)) {
            return realFileName;
        }
        return StringUtils.stripFilenameExtension(sourceFileName) + ext;
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
