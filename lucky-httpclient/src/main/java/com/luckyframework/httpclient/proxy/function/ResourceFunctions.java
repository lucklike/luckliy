package com.luckyframework.httpclient.proxy.function;


import com.luckyframework.common.FlatBean;
import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.io.ReaderInputStream;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.SerializationException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;

import static com.luckyframework.httpclient.proxy.function.CommonFunctions.getCharset;
import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.RESOURCE_FUNCTION_SPACE;

/**
 * 资源相关的函数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/3 02:19
 */
@Namespace(RESOURCE_FUNCTION_SPACE)
public class ResourceFunctions {

    /**
     * 将文件内容映射到一个{@link FlatBean}上
     * <pre>
     *   支持的资源类型有：
     *     1.properties文件
     *     2.yml文件
     *     3.yaml文件
     *     4.json文件
     *     5.xml文件（<![CDATA[<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd>]]>）
     * </pre>
     *
     * @param resourceLocation 资源路径
     * @param charset          字符编码
     * @return 与文件内容对应的一个ConfigurationMap
     */
    @FunctionAlias("read_flat_bean")
    public static FlatBean<?> readFlatBean(String resourceLocation, String... charset) {
        return Resources.resourceAsFlatBean(resourceLocation, getCharset(charset));
    }


    /**
     * 将文件对象转换为文本内容
     * <pre>
     *  支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param fileObj 文件对象
     * @param charset 字符集
     * @return 文本类型的文件内容
     * @throws IOException 读取文件时可能会出现的异常
     */
    public static String read(Object fileObj, String... charset) throws IOException {
        Charset ch = getCharset(charset);
        Reader reader;
        if (fileObj instanceof Reader) {
            reader = (Reader) fileObj;
        } else if (fileObj instanceof InputStream) {
            reader = new InputStreamReader((InputStream) fileObj, ch);
        } else if (fileObj instanceof byte[]) {
            reader = new InputStreamReader(new ByteArrayInputStream((byte[]) fileObj), ch);
        } else if (fileObj instanceof ByteBuffer) {
            reader = new InputStreamReader(new ByteArrayInputStream(((ByteBuffer) fileObj).array()), ch);
        } else if (fileObj instanceof InputStreamSource) {
            reader = new InputStreamReader(((InputStreamSource) fileObj).getInputStream(), ch);
        } else if (fileObj instanceof File) {
            reader = new InputStreamReader(Files.newInputStream(((File) fileObj).toPath()), ch);
        } else if (fileObj instanceof String) {
            reader = new InputStreamReader(resource((String) fileObj).getInputStream(), ch);
        } else {
            throw new SerializationException("file read operation object types are not supported: {}", ClassUtils.getClassName(fileObj));
        }

        return FileCopyUtils.copyToString(reader);
    }


    /**
     * 将文件路径转化为OutputStream
     *
     * @param path    文件路径
     * @param options 打开选项
     * @return 对应的OutputStream
     * @throws IOException 可能抛出IO异常
     */
    @FunctionAlias("out_stream")
    public static OutputStream outStream(String path, OpenOption... options) throws IOException {
        return Files.newOutputStream(Paths.get(path), options);
    }

    /**
     * 将资源路径转化为InputStream
     *
     * @param path 资源路径
     * @return 对应的InputStream
     */
    @FunctionAlias("resource_as_stream")
    public static InputStream resourceAsStream(String path) {
        return Resources.getResourceAsStream(path);
    }



    /**
     * 将文件路径转换为File对象
     *
     * @param path 文件路径
     * @return 对应的文件对象
     */
    public static File file(String path) {
        return new File(path);
    }

    /**
     * 将文件路径转化为InputStream
     *
     * @param path    文件路径
     * @param options 打开选项
     * @return 对应的InputStream
     * @throws IOException 可能抛出IO异常
     */
    @FunctionAlias("in_stream")
    public static InputStream inStream(String path, OpenOption... options) throws IOException {
        return Files.newInputStream(Paths.get(path), options);
    }



    /**
     * 将字符串转换为Resource对象
     *
     * @param path 资源路径
     * @return 资源对象
     */
    public static Resource resource(String path) {
        return Resources.getResource(path);
    }

    /**
     * 将字符串转数组换为Resource对象数组
     *
     * @param paths 资源路径数组
     * @return 资源对象数组
     */
    public static Resource[] resources(String... paths) {
        return Resources.getResources(paths);
    }


    /**
     * 将对象转化为输入流
     * <pre>
     *  支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param object   待加密的内容
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密后的字符串
     * @throws IOException 加密过程中可能出现的异常
     */
    @FunctionAlias("to_in_stream")
    public static InputStream toInStream(Object object, String... charsets) throws IOException {
        if (object instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) object);
        }
        if (object instanceof ByteBuffer) {
            return new ByteArrayInputStream(((ByteBuffer) object).array());
        }
        if (object instanceof String) {
            return new ByteArrayInputStream(((String) object).getBytes(getCharset(charsets)));
        }
        if (object instanceof InputStream) {
            return ((InputStream) object);
        }
        if (object instanceof InputStreamSource) {
            return ((InputStreamSource) object).getInputStream();
        }
        if (object instanceof Reader) {
            return new ReaderInputStream((Reader) object, getCharset(charsets));
        }
        if (object instanceof File) {
            return Files.newInputStream(((File) object).toPath());
        }
        throw new SerializationException("Converting '{}' type to InputStream is not supported.", ClassUtils.getClassName(object));
    }


}
