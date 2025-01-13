package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.io.FileUtils;
import com.luckyframework.io.ReaderInputStream;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.XML_SCHEME;
import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getConvertMetaType;
import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CONTENT_LENGTH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CONTENT_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_BYTE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STATUS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STREAM_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_STRING_BODY_$;

/**
 * 通用的公共函数类
 * <b>内置函数：</b><br/><br/>
 * <table>
 *     <tr>
 *         <th>函数加密</th>
 *         <th>函数描述</th>
 *         <th>示例</th>
 *     </tr>
 *     <tr>
 *         <td>{@link String} base64({@link Object})</td>
 *         <td>base64编码函数</td>
 *         <td>#{#base64('abcdefg')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} _base64(Object)</td>
 *         <td>base64解码函数</td>
 *         <td>#{#_base64('YWJjZGVmZw==')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} basicAuth(String, String)</td>
 *         <td>basicAuth编码函数</td>
 *         <td>#{#basicAuth('username', 'password‘)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} url(String, String...)</td>
 *         <td>URLEncoder编码</td>
 *         <td>#{#url('hello world!')} 或者 #{#url('hello world!', 'UTF-8')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} _url(String, String...)</td>
 *         <td>URLEncoder解码</td>
 *         <td>#{#_url('a23cb5') 或者 #{#_url('a23cb5', 'UTF-8')</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} json(Object)</td>
 *         <td>JSON序列化函数</td>
 *         <td>#{#json(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} xml(Object)</td>
 *         <td>XML序列化函数</td>
 *         <td>#{#xml(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} java(Object)</td>
 *         <td>Java对象序列化函数</td>
 *         <td>#{#java(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} form(Object)</td>
 *         <td>form表单序列化函数</td>
 *         <td>#{#form(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} protobuf(Object)</td>
 *         <td>protobuf序列化函数</td>
 *         <td>#{#protobuf(object)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} md5(Object)</td>
 *         <td>md5加密函数，英文小写</td>
 *         <td>#{#md5('abcdefg')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} MD5(Object)</td>
 *         <td>md5加密函数，英文大写</td>
 *         <td>#{#MD5('abcdefg')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} sha256(String, String)</td>
 *         <td>hmac-sha256算法加密</td>
 *         <td>#{#sha256('sasas', 'Hello world')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} uuid()</td>
 *         <td>生成UUID函数，英文小写</td>
 *         <td>#{#uuid()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} UUID()</td>
 *         <td>生成UUID函数，英文大写</td>
 *         <td>#{#UUID()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} nanoid(int...)</td>
 *         <td>生成nanoid函数</td>
 *         <td>#{#nanoid()} 或者 #{#nanoid(4)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Integer int} random(int, int)</td>
 *         <td>生成指定范围内的随机数</td>
 *         <td>#{#random(1, 100)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Integer int} randomMax(int)</td>
 *         <td>生成指定范围内的随机数，最大值为指定值</td>
 *         <td> #{#randomMax(100)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Long} time()</td>
 *         <td>获取当前时间毫秒(13位时间戳)</td>
 *         <td>#{#time()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Long} timeSec()</td>
 *         <td>获取当前时间秒(10位时间戳)</td>
 *         <td>#{#timeSec()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Date} date()</td>
 *         <td>获取当前日期({@link Date })</td>
 *         <td>#{#date()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} formatDate(Date, String)</td>
 *         <td>时间格式化</td>
 *         <td>#{#formatDate(#date(), 'yyyy-MM-dd HH:mm:ss')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMddHHmmssDate(Date)</td>
 *         <td>时间格式化(yyyy-MM-dd HH:mm:ss)</td>
 *         <td>#{#yyyyMMddHHmmssDate(#date())}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMddDate(Date)</td>
 *         <td>时间格式化(yyyyMMdd)</td>
 *         <td>#{#yyyyMMddDate(#date())}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} format(String)</td>
 *         <td>格式化当前时间</td>
 *         <td>#{#format('yyyy-MM-dd HH:mm:ss')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMddHHmmss()</td>
 *         <td>格式化当前时间(yyyy-MM-dd HH:mm:ss)</td>
 *         <td>#{#yyyyMMddHHmmss()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} yyyyMMdd()</td>
 *         <td>格式化当前时间(yyyyMMdd)</td>
 *         <td>#{#yyyyMMdd()}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Resource} resource(String)</td>
 *         <td>资源加载函数，返回{@link Resource }对象</td>
 *         <td>#{#resource('classpath:application.yml')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Resource Resource[]}resources(String...)</td>
 *         <td>资源加载函数，返回{@link Resource }数组对象</td>
 *         <td>#{#resources('classpath:application.yml', 'classpath:application.properties')}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} read(Object, String...)</td>
 *         <td>获取文件对象内容的函数</td>
 *         <td>#{#read('classpath:test.json') 或者 #{#read(#resource('http://lucklike.io/test.xml'))}}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link Object} looseBind(MethodContext, Object)</td>
 *         <td>松散绑定，将请求体内容松散绑定到方法上下问的返回结果上</td>
 *         <td>#{#lb($mc$, $body$)}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link String} lbe(String)</td>
 *         <td>获取将所选内容松散绑定到当前方法上下文方法的返回值上的SpEL表达式</td>
 *         <td>``#{#lbe('$body$.data')}``</td>
 *     </tr>
 * </table>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 15:57
 */
public class CommonFunctions {

    /**
     * base64编码
     * <pre>
     *     支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param object 待编码的内容
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 编码后的字符串
     */
    public static String base64(Object object, String ...charsets) throws IOException {
        return new String(Base64.getEncoder().encode(FileCopyUtils.copyToByteArray(toInStream(object, charsets))), getCharset(charsets));
    }

    /**
     * base64解码
     * <pre>
     *     支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param object base64编码之后的内容
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 解码后的字节数组
     */
    public static byte[] _base64(Object object, String ...charsets) throws IOException {
        return Base64.getDecoder().decode(FileCopyUtils.copyToByteArray(toInStream(object, charsets)));
    }

    /**
     * base64解码为字符串
     * <pre>
     *     支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param object base64编码之后的内容
     * @return 解码后的字符串
     */
    public static String _base64ToStr(Object object, String ...charsets) throws IOException {
        return new String(_base64(object, charsets), getCharset(charsets));
    }

    /**
     * 构建BasicAut格式的字符串
     *
     * @param username 用户名
     * @param password 密码
     * @return BasicAut格式的字符串
     */
    public static String basicAuth(String username, String password) throws IOException {
        String auth = "Basic " + username + ":" + password;
        return base64(auth);
    }

    /**
     * 将字符串按照知道个字符集进行url编码，不指定时默认使用UTF-8
     *
     * @param str     待编码的字符串
     * @param charset 字符集
     * @return 编码后的字符串
     * @throws UnsupportedEncodingException 编码过程中可能出现的异常
     */
    public static String url(String str, String... charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, getCharset(charset).name());
    }

    /**
     * 将字符串按照UTF-8字符集进行url解码
     *
     * @param str     待解码的字符串
     * @param charset 字符集
     * @return 解码后的字符串
     * @throws UnsupportedEncodingException 解码过程中可能出现的异常
     */
    public static String _url(String str, String... charset) throws UnsupportedEncodingException {
        return URLDecoder.decode(str, getCharset(charset).name());
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

    /**
     * [英文大写]
     * 对数据进行16进制编码
     *
     * @param data 数据
     * @return 16进制编码后的数据
     */
    public static String hex(byte[] data) {
        return DatatypeConverter.printHexBinary(data);
    }

    /**
     * 指定MessageDigest算法进行加密
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
     * @param data      待加密的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
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

    /**
     * 指定MessageDigest算法进行加密，返回十六进制的字符串
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
     * @param data      待加密的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String msgDigestHex(String algorithm, Object data, String... charsets) throws Exception {
        return hex(msgDigest(algorithm, data, charsets));
    }

    /**
     * 指定MessageDigest算法进行加密，返回Base64编码之后的字符串
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
     * @param data      待加密的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String msgDigestBase64(String algorithm, Object data, String... charsets) throws Exception {
        return base64(msgDigest(algorithm, data, charsets), charsets);
    }

    /**
     * 使用MD5算法进行加密
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    public static byte[] md5(Object data, String... charsets) throws Exception {
        return msgDigest("MD5", data, charsets);
    }

    /**
     * 使用MD5算法进行加密，返回十六进制的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    public static String md5Hex(Object data, String... charsets) throws Exception {
        return hex(md5(data, charsets));
    }

    /**
     * 使用MD5算法进行加密，返回Base64编码之后的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    public static String md5Base64(Object data, String... charsets) throws Exception {
        return base64(md5(data, charsets), charsets);
    }

    /**
     * 使用SHA-1算法进行加密
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    public static byte[] sha1(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-1", data, charsets);
    }

    /**
     * 使用SHA-1算法进行加密，返回十六进制的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参
     * @return 加密之后的十六进制的字符串
     */
    public static String sha1Hex(Object data, String... charsets) throws Exception {
        return hex(sha1(data, charsets));
    }

    /**
     * 使用SHA-1算法进行加密，返回Base64编码之后的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    public static String sha1Base64(Object data, String... charsets) throws Exception {
        return base64(sha1(data, charsets), charsets);
    }


    /**
     * 使用SHA-256算法进行加密
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    public static byte[] sha256(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-256", data, charsets);
    }

    /**
     * 使用SHA-256算法进行加密，返回十六进制的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    public static String sha256Hex(Object data, String... charsets) throws Exception {
        return hex(sha256(data, charsets));
    }

    /**
     * 使用SHA-256算法进行加密，返回Base64编码之后的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    public static String sha256Base64(Object data, String... charsets) throws Exception {
        return base64(sha256(data, charsets), charsets);
    }

    /**
     * 使用SHA-512算法进行加密
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    public static byte[] sha512(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-512", data, charsets);
    }

    /**
     * 使用SHA-512算法进行加密，返回十六进制的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    public static String sha512Hex(Object data, String... charsets) throws Exception {
        return hex(sha512(data, charsets));
    }

    /**
     * 使用SHA-512算法进行加密，返回Base64编码之后的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    public static String sha512Base64(Object data, String... charsets) throws Exception {
        return base64(sha512(data, charsets), charsets);
    }

    /**
     * 使用SHA-512算法进行加密
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    public static byte[] sha384(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-384", data, charsets);
    }

    /**
     * 使用SHA-384算法进行加密，返回十六进制的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    public static String sha384Hex(Object data, String... charsets) throws Exception {
        return hex(sha384(data, charsets));
    }

    /**
     * 使用SHA-384算法进行加密，返回Base64编码之后的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    public static String sha384Base64(Object data, String... charsets) throws Exception {
        return base64(sha384(data, charsets), charsets);
    }

    /**
     * 使用SHA-224算法进行加密
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     */
    public static byte[] sha224(Object data, String... charsets) throws Exception {
        return msgDigest("SHA-224", data, charsets);
    }

    /**
     * 使用SHA-224算法进行加密，返回十六进制的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     */
    public static String sha224Hex(Object data, String... charsets) throws Exception {
        return hex(sha224(data, charsets));
    }

    /**
     * 使用SHA-224算法进行加密，返回Base64编码之后的字符串
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
     * @param data     待加密的数据
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     */
    public static String sha224Base64(Object data, String... charsets) throws Exception {
        return base64(sha224(data, charsets), charsets);
    }

    /**
     * 指定Mac算法进行加密
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
     * @param secret    秘钥
     * @param data      待加密的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
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
     * 指定Mac算法进行加密，返回十六进制的字符串
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
     * @param secret    秘钥
     * @param data      待加密的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后的十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macEncryptHex(String algorithm, Object secret, Object data, String... charsets) throws Exception {
        return hex(macEncrypt(algorithm, secret, data, charsets));
    }

    /**
     * 指定Mac算法进行加密，返回Base64编码之后的字符串
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
     * @param secret    秘钥
     * @param data      待加密的数据
     * @param charsets  如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macEncryptBase64(String algorithm, Object secret, Object data, String... charsets) throws Exception {
        return base64(macEncrypt(algorithm, secret, data, charsets), charsets);
    }

    /**
     * HmacMD5算法加密
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] macMd5(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacMD5", secret, data, charsets);
    }

    /**
     * HmacMD5算法加密，返回十六进制的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macMd5Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macMd5(secret, data, charsets));
    }

    /**
     * HmacMD5算法加密，返回Base64编码之后的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macMd5Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macMd5(secret, data, charsets), charsets);
    }

    /**
     * hmac-sha1算法加密
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] macSha1(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA1", secret, data, charsets);
    }

    /**
     * hmac-sha1算法加密，返回十六进制的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha1Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha1(secret, data, charsets));
    }

    /**
     * hmac-sha1算法加密，返回Base64编码之后的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha1Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha1(secret, data, charsets), charsets);
    }

    /**
     * hmac-sha224算法加密
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] macSha224(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA224", secret, data, charsets);
    }

    /**
     * hmac-sha224算法加密，返回十六进制的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha224Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha224(secret, data, charsets));
    }

    /**
     * hmac-sha224算法加密，返回Base64编码之后的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha224Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha224(secret, data, charsets), charsets);
    }

    /**
     * hmac-sha256算法加密
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] macSha256(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA256", secret, data, charsets);
    }

    /**
     * hmac-sha256算法加密，返回十六进制的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha256Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha256(secret, data, charsets));
    }

    /**
     * hmac-sha256算法加密，返回Base64编码之后的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha256Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha256(secret, data, charsets), charsets);
    }

    /**
     * hmac-sha384算法加密
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] macSha384(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA384", secret, data, charsets);
    }

    /**
     * hmac-sha384算法加密，返回十六进制的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha384Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha384(secret, data, charsets));
    }

    /**
     * hmac-sha384算法加密，返回Base64编码之后的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha384Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha384(secret, data, charsets), charsets);
    }

    /**
     * hmac-sha512算法加密
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] macSha512(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("HmacSHA512", secret, data, charsets);
    }

    /**
     * hmac-sha512算法加密，返回十六进制的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha512Hex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macSha512(secret, data, charsets));
    }

    /**
     * hmac-sha512算法加密，返回Base64编码之后的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macSha512Base64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macSha512(secret, data, charsets), charsets);
    }

    /**
     * AES-CMAC算法加密
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] macAes(Object secret, Object data, String... charsets) throws Exception {
        return macEncrypt("AESCMAC", secret, data, charsets);
    }

    /**
     * AES-CMAC算法加密，返回十六进制的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后十六进制的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macAesHex(Object secret, Object data, String... charsets) throws Exception {
        return hex(macAes(secret, data, charsets));
    }

    /**
     * AES-CMAC算法加密，返回Base64编码之后的字符串
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
     * @param data     待加密的信息
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 加密之后Base64编码的字符串
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String macAesBase64(Object secret, Object data, String... charsets) throws Exception {
        return base64(macAes(secret, data, charsets), charsets);
    }


    /**
     * 将对象序列化为JSON字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的json字符串
     * @throws Exception 序列化过程中可能出现的异常
     */
    public static String json(Object object) throws Exception {
        return JSON_SCHEME.serialization(object);
    }

    /**
     * 将JSON字符串转成对象
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
     * @param json  JSON字符串
     * @param types 要转换的类型
     * @param <T>   转换目标对象类型
     * @return 转换后的对象
     * @throws Exception 转换过程中可能会有异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T _json(Object json, Class<T>... types) throws Exception {
        Class<?> resultClass = ContainerUtils.isEmptyArray(types) ? Object.class : types[0];

        if (json instanceof String) {
            return (T) JSON_SCHEME.deserialization((String) json, resultClass);
        }
        if (json instanceof InputStreamSource) {
            return (T) JSON_SCHEME.deserialization(new InputStreamReader(((InputStreamSource) json).getInputStream(), StandardCharsets.UTF_8), resultClass);
        }
        if (json instanceof Reader) {
            return (T) JSON_SCHEME.deserialization((Reader) json, resultClass);
        }
        if (json instanceof File) {
            return (T) JSON_SCHEME.deserialization(new FileReader((File) json), resultClass);
        }
        if (json instanceof InputStream) {
            return (T) JSON_SCHEME.deserialization(new InputStreamReader(((InputStream) json), StandardCharsets.UTF_8), resultClass);
        }
        if (json instanceof ByteBuffer) {
            return (T) JSON_SCHEME.deserialization(new String(((ByteBuffer) json).array(), StandardCharsets.UTF_8), resultClass);
        }
        if (json instanceof byte[]) {
            return (T) JSON_SCHEME.deserialization(new String((byte[]) json, StandardCharsets.UTF_8), resultClass);
        }
        throw new SerializationException("Types not supported by JSON parsing methods: {}", ClassUtils.getClassName(json));
    }

    /**
     * 将对象序列化为xml字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的xml字符串
     * @throws Exception 序列化过程中可能出现的异常
     */
    public static String xml(Object object) throws Exception {
        return XML_SCHEME.serialization(object);
    }

    /**
     * 将对象序列化为jdk序列化字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的jdk序列化字符串
     * @throws IOException 序列化过程中可能出现的异常
     */
    public static String java(Object object) throws IOException {
        return JDK_SCHEME.serialization(object);
    }

    /**
     * 将对象序列化为protobuf字节数组
     *
     * @param object 待序列化的对象
     * @return 序列化后的protobuf字节数组
     */
    public static byte[] protobuf(Object object) {
        if (object == null) {
            return new byte[0];
        }
        Class<?> objectClass = object.getClass();
        String messageClassName = "com.google.protobuf.MessageLite";
        Set<String> inheritanceStructure = ClassUtils.getInheritanceStructure(objectClass);
        if (inheritanceStructure.contains(messageClassName)) {
            return (byte[]) MethodUtils.invoke(object, "toByteArray");
        }

        throw new SerializationException("Serialization Exception: '" + objectClass + "' is not a Protobuf message type");
    }


    /**
     * 将对象序列化为表单字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的表单字符串
     */
    public static String form(Object object) {
        if (object == null) {
            return "";
        }
        ConfigurationMap configMap = new ConfigurationMap(object);
        Properties properties = configMap.toProperties(true);
        StringBuilder formBodySb = new StringBuilder();
        Enumeration<?> enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            formBodySb.append(key).append("=").append(properties.getProperty(key)).append("&");
        }
        return formBodySb.substring(0, formBodySb.length() - 1);
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
    public static InputStream inStream(String path, OpenOption... options) throws IOException {
        return Files.newInputStream(Paths.get(path), options);
    }

    /**
     * 将文件路径转化为OutputStream
     *
     * @param path    文件路径
     * @param options 打开选项
     * @return 对应的OutputStream
     * @throws IOException 可能抛出IO异常
     */
    public static OutputStream outStream(String path, OpenOption... options) throws IOException {
        return Files.newOutputStream(Paths.get(path), options);
    }

    /**
     * 将资源路径转化为InputStream
     *
     * @param path 资源路径
     * @return 对应的InputStream
     */
    public static InputStream resourceAsStream(String path) {
        return Resources.getResourceAsStream(path);
    }

    /**
     * 产生一个随机的UUID
     *
     * @return UUID
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 【英文大写】产生一个随机的UUID
     *
     * @return UUID
     */
    public static String UUID() {
        return uuid().toUpperCase();
    }

    /**
     * 产生一个随机的NanoId
     *
     * @return NanoId
     */
    public static String nanoid(Integer... length) {
        int len = ContainerUtils.isEmptyArray(length) ? 21 : length[0];
        return NanoIdUtils.randomNanoId(len);
    }

    /**
     * 生成一个指定范围内的随机整数
     *
     * @param min 最小值
     * @param max 最大值
     * @return 指定范围内的随机整数
     */
    public static int random(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    /**
     * 生成一个0-max的随机整数
     *
     * @param max 最大值
     * @return 0-max的随机整数
     */
    public static int randomMax(int max) {
        return random(0, max);
    }

    /**
     * 获取当前时间毫秒(13位时间戳)
     *
     * @return 当前时间毫秒
     */
    public static long time() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间秒(10位时间戳)
     *
     * @return 当前时间毫秒
     */
    public static long timeSec() {
        return time() / 1000L;
    }

    /**
     * 获取当前时间{@link Date}
     *
     * @return 当前时间
     */
    public static Date date() {
        return new Date();
    }

    /**
     * 时间格式化
     *
     * @param date   时间
     * @param format 时间格式
     * @return 格式化后的时间
     */
    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 时间格式化【yyyy-MM-dd HH:mm:ss】
     *
     * @param date 时间
     * @return 格式化后的时间
     */
    public static String yyyyMMddHHmmssDate(Date date) {
        return formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 时间格式化【yyyyMMdd】
     *
     * @param date 时间
     * @return 格式化后的时间
     */
    public static String yyyyMMddDate(Date date) {
        return formatDate(date, "yyyyMMdd");
    }

    /**
     * 格式化当前时间
     *
     * @param format 时间格式
     * @return 格式化后的时间
     */
    public static String formatNow(String format) {
        return formatDate(date(), format);
    }

    /**
     * 格式化当前时间【yyyy-MM-dd HH:mm:ss】
     *
     * @return 格式化后的时间
     */
    public static String yyyyMMddHHmmss() {
        return yyyyMMddHHmmssDate(date());
    }

    /**
     * 格式化当前时间【yyyyMMdd】
     *
     * @return 格式化后的时间
     */
    public static String yyyyMMdd() {
        return yyyyMMddDate(date());
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
     * 松散绑定，将请求体内容松散绑定到方法上下问的返回结果上
     * <pre>
     *     lb方法名含义（松散绑定）
     *     l: loose
     *     b: bind
     * </pre>
     *
     * @param mc   方法上下文
     * @param body 请求体对象
     * @return 松散绑定后的结果
     */
    public static Object lb(MethodContext mc, Object body) {
        return ConversionUtils.looseBind(mc.getRealMethodReturnType(), body);
    }

    /**
     * 获取将所选内容松散绑定到当前方法上下文方法的返回值上的SpEL表达式
     * <pre>
     *     lbe方法名含义（松散绑定表达式）
     *     l: loose
     *     b: bind
     *     e: expression
     *
     *     注意：
     *      此方法其实与{@link #lb(MethodContext, Object)}方法是等价的，
     *      此方法的返回值为一个String字符串，该字符串的本质就是一个SpEL表达式，其作用
     *      就是调用{@link #lb(MethodContext, Object)}方法。
     *
     *      用法：（需要结合嵌套解析语法一起使用）
     *      {@code
     *          ``#{#lbe('$body$.data')}``
     *      }
     * </pre>
     *
     * @param bodySelect 响应体内容选择表达式
     * @return 将所选内容松散绑定到当前方法上下文方法的返回值上的SpEL表达式
     */
    public static String lbe(String bodySelect) {
        return StringUtils.format("#{#lb($mc$, '{}')}", bodySelect);
    }

    /**
     * 检查给定的字符串是否包含实际文本。更具体地说，如果String不为空，其长度大于0，并且至少包含一个非空白字符，则此方法返回true。
     *
     * @param txt 待检测的字符串
     * @return 是否包含实际的文本
     */
    public static boolean hasText(String txt) {
        return StringUtils.hasText(txt);
    }

    /**
     * 检查给定的字符串是否不包含实际文本
     *
     * @param txt 待检测的字符串
     * @return 是否不包含实际的文本
     */
    public static boolean nonText(String txt) {
        return !hasText(txt);
    }

    /**
     * 检查给定的对象是否为null
     *
     * @param obj 待检测的对象
     * @return 是否为null
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 检查给定的对象是否不为null
     *
     * @param obj 待检测的对象
     * @return 是否不为null
     */
    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    /**
     * 字符串格式化合成
     *
     * @param format 模版
     * @param args   参数
     * @return 合成的字符串
     */
    public static String str(String format, Object... args) {
        return StringUtils.format(format, args);
    }

    /**
     * 检查是否为null
     * <pre>
     *     null         -> true
     *     String       -> {@link StringUtils#hasText(String)}
     *     Collection   -> {@link Collection#isEmpty()}
     *     Map          -> {@link Map#isEmpty()}
     *     Array        -> {@link Array#getLength(Object)}
     * </pre>
     *
     * @param obj 带检测的对象
     * @return 是否是空集合
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof String) {
            return hasText((String) obj);
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        throw new IllegalArgumentException(obj.getClass().getName() + " is not a string or collection or array or map.");
    }

    /**
     * 检查是否不为null
     *
     * @param obj 带检测的对象
     * @return 是否是空集合
     */
    public static boolean nonEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断某个元素是否在集合中
     * <pre>
     *     collection=null || element=null -> false
     *     String       -> {@link String#contains(CharSequence)}
     *     Collection   -> {@link Collection#contains(Object)}
     *     Map          -> {@link Map#containsKey(Object)}
     *     Array        -> 循环比较
     *
     * </pre>
     *
     * @param collection 集合
     * @param element    元素
     * @return 元素是否在集合中
     */
    public static boolean in(Object collection, Object element) {
        if (collection == null || element == null) {
            return false;
        }
        if (collection instanceof String && element instanceof CharSequence) {
            return ((String) collection).contains(((String) element));
        }
        if (collection instanceof Collection) {
            return ((Collection<?>) collection).contains(element);
        }
        if (collection instanceof Map) {
            return ((Map<?, ?>) collection).containsKey(element);
        }
        if (collection.getClass().isArray()) {
            int length = Array.getLength(collection);
            for (int i = 0; i < length; i++) {
                if (Objects.equals(element, Array.get(collection, i))) {
                    return true;
                }
            }
            return false;
        }
        throw new IllegalArgumentException(collection.getClass().getName() + " is not a string or collection or array or map.");
    }

    /**
     * 判断某个元素是否不在集合中
     *
     * @param collection 集合
     * @param element    元素
     * @return 元素是否不在集合中
     */
    public static boolean nonIn(Object collection, Object element) {
        return !in(collection, element);
    }

    /**
     * 获取注解实例
     *
     * @param mc             上下文对象
     * @param annotationName 注解全类名
     * @return 注解实例
     * @throws ClassNotFoundException 对应的注解不存在时会抛出该异常
     */
    @SuppressWarnings("unchecked")
    public static Annotation ann(Context mc, String annotationName) throws ClassNotFoundException {
        return mc.getMergedAnnotationCheckParent((Class<? extends Annotation>) Class.forName(annotationName));
    }

    /**
     * 获取注解实例
     *
     * @param mc             上下文对象
     * @param annotationType 注解Class
     * @return 注解实例
     */
    public static <A extends Annotation> A annc(Context mc, Class<A> annotationType) {
        return mc.getMergedAnnotationCheckParent(annotationType);
    }

    /**
     * 判断方法上是否存在某个注解
     *
     * @param mc             上下文对象
     * @param annotationName 注解全类名
     * @return 方法上是否存在该注解
     * @throws ClassNotFoundException 对应的注解不存在时会抛出该异常
     */
    @SuppressWarnings("unchecked")
    public static boolean hasAnn(Context mc, String annotationName) throws ClassNotFoundException {
        return mc.isAnnotated((Class<? extends Annotation>) Class.forName(annotationName));
    }

    /**
     * 判断方法上是否存在某个注解
     *
     * @param mc             上下文对象
     * @param annotationType 注解Class
     * @return 方法上是否存在该注解
     */
    public static boolean hasAnnc(Context mc, Class<? extends Annotation> annotationType) {
        return mc.isAnnotated(annotationType);
    }

    /**
     * 将响应对象转化为标准Map格式
     *
     * @param response 响应对象
     * @param context  上下文对象
     * @return 标准Map格式
     */
    public static Map<String, Object> sta(Response response, Context context) {
        Map<String, Object> map = new HashMap<>();
        map.put($_RESPONSE_$, LazyValue.of(response));
        map.put($_RESPONSE_STATUS_$, LazyValue.of(response::getStatus));
        map.put($_CONTENT_LENGTH_$, LazyValue.of(response::getContentLength));
        map.put($_CONTENT_TYPE_$, LazyValue.of(response::getContentType));
        map.put($_RESPONSE_HEADER_$, LazyValue.of(response::getSimpleHeaders));
        map.put($_RESPONSE_COOKIE_$, LazyValue.of(response::getSimpleCookies));
        map.put($_RESPONSE_STREAM_BODY_$, LazyValue.rtc(response::getInputStream));
        map.put($_RESPONSE_STRING_BODY_$, LazyValue.of(response::getStringResult));
        map.put($_RESPONSE_BYTE_BODY_$, LazyValue.of(response::getResult));
        map.put($_RESPONSE_BODY_$, LazyValue.of(() -> getResponseBody(response, () -> getConvertMetaType(context))));
        return map;
    }

    @FunctionFilter
    private static Charset getCharset(String... charset) {
        return ContainerUtils.isEmptyArray(charset) ? StandardCharsets.UTF_8 : Charset.forName(charset[0]);
    }


}
