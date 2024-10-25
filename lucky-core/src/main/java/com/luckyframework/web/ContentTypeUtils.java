package com.luckyframework.web;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * ContentType-Type工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/5 10:26
 */
public class ContentTypeUtils {

    /**
     * 根据文件名获取对应的MimeType
     * @param fileName 待文件类型后缀的文件名
     * @return 文件对应的MimeType
     */
    @Nullable
    public static String getMimeType(@NonNull String fileName) {
        ContentInfo contentInfo = ContentInfoUtil.findExtensionMatch(fileName);
        return contentInfo == null ?  null : contentInfo.getMimeType();
    }

    /**
     * 根据输入流来获取MimeType
     * @param in 输入流
     * @return 对应的MimeType
     */
    public static String getMimeType(@NonNull InputStream in) throws IOException {
        ContentInfoUtil contentInfoUtil = new ContentInfoUtil();
        ContentInfo contentInfo = contentInfoUtil.findMatch(in);
        return contentInfo == null ?  null : contentInfo.getMimeType();
    }

    /**
     * 根据文件获取MimeType
     * @param file 输入流
     * @return 对应的MimeType
     */
    public static String getMimeType(@NonNull File file) throws IOException {
        ContentInfoUtil contentInfoUtil = new ContentInfoUtil();
        ContentInfo contentInfo = contentInfoUtil.findMatch(file);
        return contentInfo == null ?  null : contentInfo.getMimeType();
    }

    /**
     * 根据byte[]获取MimeType
     * @param bytes 输入流
     * @return 对应的MimeType
     */
    public static String getMimeType(@NonNull byte[] bytes) throws IOException {
        ContentInfoUtil contentInfoUtil = new ContentInfoUtil();
        ContentInfo contentInfo = contentInfoUtil.findMatch(bytes);
        return contentInfo == null ?  null : contentInfo.getMimeType();
    }

    /**
     * 根据文件名获取对应的MimeType，如果遇到不支持的文件类型则返回默认值
     * @param fileName 待文件类型后缀的文件名
     * @param defaultMimeType 默认的MimeType
     * @return 文件对应的MimeType
     */
    public static String getMimeTypeOrDefault(@NonNull String fileName, @NonNull String defaultMimeType) {
        ContentInfo contentInfo = ContentInfoUtil.findExtensionMatch(fileName);
        return contentInfo == null ?  defaultMimeType : contentInfo.getMimeType();
    }

    /**
     * 根据mimeTyp获取对应的文件扩展名
     * @param mimeTyp mimeTyp
     * @return 对应的文件扩展名
     */
    public static String getFileExtension(@NonNull String mimeTyp) {
        ContentInfo contentInfo = ContentInfoUtil.findMimeTypeMatch(mimeTyp);
        return contentInfo == null ? null : contentInfo.getFileExtensions()[0];
    }

}
