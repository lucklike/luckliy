package com.luckyframework.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 可重复读取流工具类
 */
public class RepeatableReadStreamUtil {

    //-----------------------------------------------------------------------
    //                      Repeatable Read Stream
    //-----------------------------------------------------------------------

    /**
     * 将某个输入流转化为基于byte数组存储的可重复读输入流
     *
     * @param in 原始输入流
     * @return 于byte数组存储的可重复读输入流
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static RepeatableReadByteInputStream useByteStore(InputStream in) throws IOException {
        return new RepeatableReadByteInputStream(in);
    }

    /***
     * 将某个输入流转化为基于本地文件存储的可重复读输入流
     *
     * @param in 原始输入流
     * @return 基于本地文件存储的可重复读输入流
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static RepeatableReadFileInputStream useFileStore(InputStream in) throws IOException {
        return new RepeatableReadFileInputStream(in);
    }

    /**
     * 将某个输入流转化为基于本地文件存储的可重复读输入流，指定本地存储文件
     *
     * @param storeFile 指定的本地存储文件
     * @param in        原始输入流
     * @return 基于指定本地文件存储的可重复读输入流
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static RepeatableReadFileInputStream useFileStore(File storeFile, InputStream in) throws IOException {
        return new RepeatableReadFileInputStream(storeFile, in);
    }

    /**
     * 将某个输入流转化为基于本地文件存储的可重复读输入流，指定本地存储文件路径
     *
     * @param storeFilePath 指定的本地存储文件路径
     * @param in            原始输入流
     * @return 基于指定本地文件存储的可重复读输入流
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static RepeatableReadFileInputStream useFileStore(String storeFilePath, InputStream in) throws IOException {
        return new RepeatableReadFileInputStream(storeFilePath, in);
    }

    //-----------------------------------------------------------------------
    //                      Repeatable Read Stream Source
    //-----------------------------------------------------------------------

    /**
     * 将某个输入流转化为基于byte数组存储的可重复读输入流源
     *
     * @param in 原始输入流
     * @return 于byte数组存储的可重复读输入流源
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static ByteStorageInputStreamSource useByteSource(InputStream in) throws IOException {
        return new ByteStorageInputStreamSource(in);
    }

    /***
     * 将某个输入流转化为基于本地文件存储的可重复读输入流源
     *
     * @param in 原始输入流
     * @return 基于本地文件存储的可重复读输入流源
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static FileStorageInputStreamSource useFileSource(InputStream in) throws IOException {
        return new FileStorageInputStreamSource(in);
    }

    /**
     * 将某个输入流转化为基于本地文件存储的可重复读输入流源，指定本地存储文件
     *
     * @param storeFile 指定的本地存储文件
     * @param in        原始输入流
     * @return 基于指定本地文件存储的可重复读输入流源
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static FileStorageInputStreamSource useFileSource(File storeFile, InputStream in) throws IOException {
        return new FileStorageInputStreamSource(storeFile, in);
    }

    /**
     * 将某个输入流转化为基于本地文件存储的可重复读输入流源，指定本地存储文件路径
     *
     * @param storeFilePath 指定的本地存储文件路径
     * @param in            原始输入流
     * @return 基于指定本地文件存储的可重复读输入流源
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static FileStorageInputStreamSource useFileSource(String storeFilePath, InputStream in) throws IOException {
        return new FileStorageInputStreamSource(storeFilePath, in);
    }
}
