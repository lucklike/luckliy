package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.io.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.function.Function;

/**
 * 文件类型转换函数
 */
public class FileTypeConvertFunction implements Function<File, Object> {

    private final MethodContext methodContext;

    public FileTypeConvertFunction(MethodContext methodContext) {
        this.methodContext = methodContext;
    }

    public static void convertTypeCheck(MethodContext methodContext, String errMsg) {
        // 封装返回值
        if (methodContext.isVoidMethod()) {
            return;
        }
        Type returnType = methodContext.getRealMethodReturnType();

        // Boolean类型返回值时返回true
        if (returnType == Boolean.class || returnType == boolean.class) {
            return;
        }
        // File类型返回值时返回文件对象
        if (returnType == File.class) {
            return;
        }
        // Long类返回值时返回文件大小
        if (returnType == Long.class || returnType == long.class) {
            return;
        }
        // String类型返回值文件的绝对路径
        if (returnType == String.class) {
            return;
        }
        // InputStream类型返回值时返回对应的文件输入流
        if (returnType == InputStream.class) {
            return;
        }
        // MultipartFile类型返回值
        if (returnType == MultipartFile.class) {
            return ;
        }
        throw new FileConvertException(errMsg, returnType);
    }


    @Override
    public Object apply(File file) {
        // 封装返回值
        if (methodContext.isVoidMethod()) {
            return null;
        }
        Type returnType = methodContext.getRealMethodReturnType();

        // Boolean类型返回值时返回true
        if (returnType == Boolean.class || returnType == boolean.class) {
            return Boolean.TRUE;
        }
        // File类型返回值时返回文件对象
        if (returnType == File.class) {
            return file;
        }
        // Long类返回值时返回文件大小
        if (returnType == Long.class || returnType == long.class) {
            return file.length();
        }
        // String类型返回值文件的绝对路径
        if (returnType == String.class) {
            return file.getAbsolutePath();
        }
        // InputStream类型返回值时返回对应的文件输入流
        if (returnType == InputStream.class) {
            try {
                return Files.newInputStream(file.toPath());
            } catch (IOException e) {
                throw new FileConvertException(e, "Failed to get file input stream from file '{}'.", file.getAbsolutePath());
            }

        }
        // MultipartFile类型返回值
        if (returnType == MultipartFile.class) {
            return new MultipartFile(file);
        }
        throw new FileConvertException("Unsupported method return value type: {}", returnType);
    }
}
