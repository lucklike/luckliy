package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.io.FileUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

import static com.luckyframework.httpclient.proxy.function.CommonFunctions.toInStream;
import static com.luckyframework.httpclient.proxy.function.SerializationFunctions.base64;
import static com.luckyframework.httpclient.proxy.function.SerializationFunctions.hex;
import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.CRYPTO_MAC_FUNCTION_SPACE;

/**
 * {@link Mac}消息认证相关的函数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/3 02:49
 */
@Namespace(CRYPTO_MAC_FUNCTION_SPACE)
public class MacFunctions {

    //---------------------------------------------------------------------------
    //                               AESCMAC
    //---------------------------------------------------------------------------


    /**
     * AES-CMAC算法消息摘要
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_aes")
    public static byte[] macAes(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("AESCMAC", secret, data, charsets);
    }

    /**
     * AES-CMAC算法消息摘要，返回十六进制的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_aes_hex")
    public static String macAesHex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macAes(secret, data, charsets));
    }

    /**
     * AES-CMAC算法消息摘要，返回Base64编码之后的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_aes_base64")
    public static String macAesBase64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macAes(secret, data, charsets), charsets);
    }



    //---------------------------------------------------------------------------
    //                               HmacSHA512
    //---------------------------------------------------------------------------


    /**
     * hmac-sha512算法消息摘要
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha512")
    public static byte[] macSha512(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA512", secret, data, charsets);
    }

    /**
     * hmac-sha512算法消息摘要，返回十六进制的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha512_hex")
    public static String macSha512Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha512(secret, data, charsets));
    }

    /**
     * hmac-sha512算法消息摘要，返回Base64编码之后的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha512_base64")
    public static String macSha512Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha512(secret, data, charsets), charsets);
    }
    
    

    //---------------------------------------------------------------------------
    //                               HmacSHA384
    //---------------------------------------------------------------------------


    /**
     * hmac-sha384算法消息摘要
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha384")
    public static byte[] macSha384(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA384", secret, data, charsets);
    }

    /**
     * hmac-sha384算法消息摘要，返回十六进制的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha384_hex")
    public static String macSha384Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha384(secret, data, charsets));
    }

    /**
     * hmac-sha384算法消息摘要，返回Base64编码之后的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha384_base64")
    public static String macSha384Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha384(secret, data, charsets), charsets);
    }



    //---------------------------------------------------------------------------
    //                               HmacSHA256
    //---------------------------------------------------------------------------


    /**
     * hmac-sha256算法消息摘要
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha256")
    public static byte[] macSha256(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA256", secret, data, charsets);
    }

    /**
     * hmac-sha256算法消息摘要，返回十六进制的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha256_hex")
    public static String macSha256Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha256(secret, data, charsets));
    }

    /**
     * hmac-sha256算法消息摘要，返回Base64编码之后的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha256_base64")
    public static String macSha256Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha256(secret, data, charsets), charsets);
    }


    //---------------------------------------------------------------------------
    //                               HmacSHA224
    //---------------------------------------------------------------------------



    /**
     * hmac-sha224算法消息摘要
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha224")
    public static byte[] macSha224(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA224", secret, data, charsets);
    }

    /**
     * hmac-sha224算法消息摘要，返回十六进制的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha224_hex")
    public static String macSha224Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha224(secret, data, charsets));
    }

    /**
     * hmac-sha224算法消息摘要，返回Base64编码之后的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha224_base64")
    public static String macSha224Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha224(secret, data, charsets), charsets);
    }
    

    //---------------------------------------------------------------------------
    //                               HmacSHA1
    //---------------------------------------------------------------------------


    /**
     * hmac-sha1算法消息摘要
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha1")
    public static byte[] macSha1(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA1", secret, data, charsets);
    }

    /**
     * hmac-sha1算法消息摘要，返回十六进制的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha1_hex")
    public static String macSha1Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha1(secret, data, charsets));
    }

    /**
     * hmac-sha1算法消息摘要，返回Base64编码之后的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_sha1_base64")
    public static String macSha1Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha1(secret, data, charsets), charsets);
    }
    


    //---------------------------------------------------------------------------
    //                               HmacMD5
    //---------------------------------------------------------------------------

    /**
     * HmacMD5算法消息摘要
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_md5")
    public static byte[] macMd5(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacMD5", secret, data, charsets);
    }

    /**
     * HmacMD5算法消息摘要，返回十六进制的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_md5_hex")
    public static String macMd5Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macMd5(secret, data, charsets));
    }

    /**
     * HmacMD5算法消息摘要，返回Base64编码之后的字符串
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
     * @param secret   秘钥
     * @param data     待进行消息摘要的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_md5_base64")
    public static String macMd5Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macMd5(secret, data, charsets), charsets);
    }


    //---------------------------------------------------------------------------
    //                               Basic
    //---------------------------------------------------------------------------

    /**
     * 指定Mac算法进行消息摘要
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
     * @param algorithm 消息摘要算法
     * @param secret    秘钥
     * @param data      待进行消息摘要的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的字节数组
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_encrypt")
    public static byte[] macEncrypt(String algorithm, Object secret, Object data, String... charsets) throws Exception {
        InputStream secretIn = toInStream(secret, "UTF-8");
        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec spec = new SecretKeySpec(FileCopyUtils.copyToByteArray(secretIn), algorithm);
        mac.init(spec);

        InputStream dataIn = toInStream(data, charsets);
        byte[] buffer = new byte[FileCopyUtils.BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = dataIn.read(buffer)) != -1) {
            mac.update(buffer, 0, bytesRead);
        }
        FileUtils.closeIgnoreException(dataIn);
        return mac.doFinal();
    }

    /**
     * 指定Mac算法进行消息摘要，返回十六进制的字符串
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
     * @param algorithm 消息摘要算法
     * @param secret    秘钥
     * @param data      待进行消息摘要的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后的十六进制的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_encrypt_hex")
    public static String macEncryptHex(String algorithm, Object secret, Object data, String... charsets) throws Exception {
        return hex(macEncrypt(algorithm, secret, data, charsets));
    }

    /**
     * 指定Mac算法进行消息摘要，返回Base64编码之后的字符串
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
     * @param algorithm 消息摘要算法
     * @param secret    秘钥
     * @param data      待进行消息摘要的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 消息摘要之后Base64编码的字符串
     * @throws Exception 消息摘要过程中可能出现的异常
     */
    @FunctionAlias("mac_encrypt_base64")
    public static String macEncryptBase64(String algorithm, Object secret, Object data, String... charsets) throws Exception {
        return base64(macEncrypt(algorithm, secret, data, charsets), charsets);
    }

}
