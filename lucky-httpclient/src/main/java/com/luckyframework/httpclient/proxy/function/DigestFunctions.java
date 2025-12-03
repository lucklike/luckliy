package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.io.FileUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static com.luckyframework.httpclient.proxy.function.ResourceFunctions.toInStream;
import static com.luckyframework.httpclient.proxy.function.SerializationFunctions.base64;
import static com.luckyframework.httpclient.proxy.function.SerializationFunctions.hex;
import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.MESSAGE_DIGEST_FUNCTION_SPACE;

/**
 * {@link MessageDigest}消息摘要相关的函数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/3 02:19
 */
@Namespace(MESSAGE_DIGEST_FUNCTION_SPACE)
public class DigestFunctions {

    //---------------------------------------------------------------------------
    //                                  SHA-224
    //---------------------------------------------------------------------------


    /**
     * 使用SHA-224算法进行消息摘要
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    @FunctionAlias("sha224")
    public static byte[] sha224(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-224", data, charsets);
    }

    /**
     * 使用SHA-224算法进行消息摘要，返回十六进制的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    @FunctionAlias("sha224_hex")
    public static String sha224Hex(Object data, String... charsets) throws Exception {
        return hex(sha224(data, charsets));
    }

    /**
     * 使用SHA-224算法进行消息摘要，返回Base64编码之后的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    @FunctionAlias("sha224_base64")
    public static String sha224Base64(Object data, String... charsets) throws Exception {
        return base64(sha224(data, charsets), charsets);
    }


    //---------------------------------------------------------------------------
    //                                  SHA-384
    //---------------------------------------------------------------------------



    /**
     * 使用SHA-384算法进行消息摘要
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    @FunctionAlias("sha384")
    public static byte[] sha384(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-384", data, charsets);
    }

    /**
     * 使用SHA-384算法进行消息摘要，返回十六进制的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    @FunctionAlias("sha384_hex")
    public static String sha384Hex(Object data, String... charsets) throws Exception {
        return hex(sha384(data, charsets));
    }

    /**
     * 使用SHA-384算法进行消息摘要，返回Base64编码之后的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    @FunctionAlias("sha384_base64")
    public static String sha384Base64(Object data, String... charsets) throws Exception {
        return base64(sha384(data, charsets), charsets);
    }

    //---------------------------------------------------------------------------
    //                                  SHA-512
    //---------------------------------------------------------------------------

    /**
     * 使用SHA-512算法进行消息摘要
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    @FunctionAlias("sha512")
    public static byte[] sha512(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-512", data, charsets);
    }

    /**
     * 使用SHA-512算法进行消息摘要，返回十六进制的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    @FunctionAlias("sha512_hex")
    public static String sha512Hex(Object data, String... charsets) throws Exception {
        return hex(sha512(data, charsets));
    }

    /**
     * 使用SHA-512算法进行消息摘要，返回Base64编码之后的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    @FunctionAlias("sha512_base64")
    public static String sha512Base64(Object data, String... charsets) throws Exception {
        return base64(sha512(data, charsets), charsets);
    }
    

    //---------------------------------------------------------------------------
    //                                  SHA-25
    //---------------------------------------------------------------------------


    /**
     * 使用SHA-256算法进行消息摘要
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    @FunctionAlias("sha256")
    public static byte[] sha256(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-256", data, charsets);
    }

    /**
     * 使用SHA-256算法进行消息摘要，返回十六进制的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    @FunctionAlias("sha256_hex")
    public static String sha256Hex(Object data, String... charsets) throws Exception {
        return hex(sha256(data, charsets));
    }

    /**
     * 使用SHA-256算法进行消息摘要，返回Base64编码之后的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    @FunctionAlias("sha256_base64")
    public static String sha256Base64(Object data, String... charsets) throws Exception {
        return base64(sha256(data, charsets), charsets);
    }

    //---------------------------------------------------------------------------
    //                                  SHA-1
    //---------------------------------------------------------------------------


    /**
     * 使用SHA-1算法进行消息摘要
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    @FunctionAlias("sha1")
    public static byte[] sha1(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-1", data, charsets);
    }

    /**
     * 使用SHA-1算法进行消息摘要，返回十六进制的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参
     * @return 加密之后的十六进制的字符串
     */
    @FunctionAlias("sha1_hex")
    public static String sha1Hex(Object data, String... charsets) throws Exception {
        return hex(sha1(data, charsets));
    }

    /**
     * 使用SHA-1算法进行消息摘要，返回Base64编码之后的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    @FunctionAlias("sha1_base64")
    public static String sha1Base64(Object data, String... charsets) throws Exception {
        return base64(sha1(data, charsets), charsets);
    }


    //---------------------------------------------------------------------------
    //                                  MD5
    //---------------------------------------------------------------------------

    /**
     * 使用MD5算法进行消息摘要，返回Base64编码之后的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    @FunctionAlias("md5_base64")
    public static String md5Base64(Object data, String... charsets) throws Exception {
        return base64(md5(data, charsets), charsets);
    }

    /**
     * 使用MD5算法进行消息摘要，返回十六进制的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    @FunctionAlias("md5_hex")
    public static String md5Hex(Object data, String... charsets) throws Exception {
        return hex(md5(data, charsets));
    }

    /**
     * 使用MD5算法进行消息摘要
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param data     待进行消息摘要的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    @FunctionAlias("md5")
    public static byte[] md5(Object data, String... charsets) throws Exception {
        return msgDigest("MD5", data, charsets);
    }

    /**
     * 指定MessageDigest算法进行消息摘要，返回Base64编码之后的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param algorithm 加密算法
     * @param data      待进行消息摘要的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    @FunctionAlias("msg_digest_base64")
    public static String msgDigestBase64(String algorithm, Object data, String... charsets) throws Exception {
        return base64(msgDigest(algorithm, data, charsets), charsets);
    }

    //---------------------------------------------------------------------------
    //                               Basic
    //---------------------------------------------------------------------------

    /**
     * 指定MessageDigest算法进行消息摘要，返回十六进制的字符串
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param algorithm 加密算法
     * @param data      待进行消息摘要的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    @FunctionAlias("msg_digest_hex")
    public static String msgDigestHex(String algorithm, Object data, String... charsets) throws Exception {
        return hex(msgDigest(algorithm, data, charsets));
    }


    /**
     * 指定MessageDigest算法进行消息摘要
     * <pre>
     *  data支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param algorithm 加密算法
     * @param data      待进行消息摘要的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    @FunctionAlias("msg_digest")
    public static byte[] msgDigest(String algorithm, Object data, String... charsets) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);

        InputStream dataIn = toInStream(data, charsets);
        byte[] buffer = new byte[FileCopyUtils.BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = dataIn.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }

        FileUtils.closeIgnoreException(dataIn);
        return md.digest();
    }
}
