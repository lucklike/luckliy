package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.XML_SCHEME;

/**
 * Http客户端代理对象生成工厂
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
     * @return 编码后的字符串
     */
    public static String base64(Object object) throws IOException {
        byte[] encode;
        if (object instanceof String) {
            encode = ((String) object).getBytes(StandardCharsets.UTF_8);
        } else if (object instanceof byte[]) {
            encode = (byte[]) object;
        } else if (object instanceof ByteBuffer) {
            encode = ((ByteBuffer) object).array();
        } else if (object instanceof InputStream) {
            encode = FileCopyUtils.copyToByteArray((InputStream) object);
        } else if (object instanceof File) {
            encode = FileCopyUtils.copyToByteArray((File) object);
        } else if (object instanceof InputStreamSource) {
            encode = FileCopyUtils.copyToByteArray(((InputStreamSource) object).getInputStream());
        } else if (object instanceof Reader) {
            encode = FileCopyUtils.copyToString((Reader) object).getBytes(StandardCharsets.UTF_8);
        } else {
            throw new SerializationException("base64 encoded object types are not supported: {}", ClassUtils.getClassName(object));
        }
        return new String(Base64.getEncoder().encode(encode));
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
     * @return 解码后的字符串
     */
    public static String _base64(Object object) throws IOException {
        byte[] decode;
        if (object instanceof String) {
            decode = ((String) object).getBytes(StandardCharsets.UTF_8);
        } else if (object instanceof byte[]) {
            decode = (byte[]) object;
        } else if (object instanceof ByteBuffer) {
            decode = ((ByteBuffer) object).array();
        } else if (object instanceof InputStream) {
            decode = FileCopyUtils.copyToByteArray((InputStream) object);
        } else if (object instanceof File) {
            decode = FileCopyUtils.copyToByteArray((File) object);
        } else if (object instanceof InputStreamSource) {
            decode = FileCopyUtils.copyToByteArray(((InputStreamSource) object).getInputStream());
        } else if (object instanceof Reader) {
            decode = FileCopyUtils.copyToString((Reader) object).getBytes(StandardCharsets.UTF_8);
        } else {
            throw new SerializationException("base64 encoded object types are not supported: {}", ClassUtils.getClassName(object));
        }
        return new String(Base64.getDecoder().decode(decode));
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
     * 【英文小写】 md5加密
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
     * @param object 待加密的内容
     * @return 编码后的字符串
     * @throws IOException 加密过程中可能出现的异常
     */
    public static String md5(Object object) throws IOException {
        if (object instanceof byte[]) {
            return DigestUtils.md5DigestAsHex((byte[]) object);
        }
        if (object instanceof ByteBuffer) {
            return DigestUtils.md5DigestAsHex(((ByteBuffer) object).array());
        }
        if (object instanceof String) {
            return DigestUtils.md5DigestAsHex(((String) object).getBytes(StandardCharsets.UTF_8));
        }
        if (object instanceof InputStream) {
            return DigestUtils.md5DigestAsHex((InputStream) object);
        }
        if (object instanceof InputStreamSource) {
            return DigestUtils.md5DigestAsHex(((InputStreamSource) object).getInputStream());
        }
        if (object instanceof Reader) {
            return DigestUtils.md5DigestAsHex(FileCopyUtils.copyToString((Reader) object).getBytes(StandardCharsets.UTF_8));
        }
        if (object instanceof File) {
            return DigestUtils.md5DigestAsHex(Files.newInputStream(((File) object).toPath()));
        }
        throw new SerializationException("md5 encipher object types are not supported: {}", ClassUtils.getClassName(object));
    }

    /**
     * 【英文大写】 md5加密
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
     * @param object 待加密的内容
     * @return 编码后的字符串
     * @throws IOException 加密过程中可能出现的异常
     */
    public static String MD5(Object object) throws IOException {
        return md5(object).toUpperCase();
    }

    /**
     * hmac-sha256算法签名
     *
     * @param secret  秘钥
     * @param message 待签名的信息
     * @return 签名之后的字节数组
     * @throws Exception 加密过程中可能出现的异常
     */
    public static byte[] sha256(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将对象序列化为json字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的json字符串
     * @throws Exception 序列化过程中可能出现的异常
     */
    public static String json(Object object) throws Exception {
        return JSON_SCHEME.serialization(object);
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
    public static String format(String format) {
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
        return StringUtils.format("#{#lb($mc$, {})}", bodySelect);
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

    private static Charset getCharset(String... charset) {
        return ContainerUtils.isEmptyArray(charset) ? StandardCharsets.UTF_8 : Charset.forName(charset[0]);
    }

}
