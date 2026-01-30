package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.core.util.BeanUtils;
import com.luckyframework.httpclient.proxy.Version;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.ConvertMetaData;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.io.RepeatableReadByteInputStream;
import com.luckyframework.io.RepeatableReadFileInputStream;
import com.luckyframework.io.RepeatableReadStreamUtil;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.luckyframework.httpclient.proxy.function.SerializationFunctions.base64;
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
import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.COMMON_FUNCTION_SPACE;

/**
 * 通用的公共函数类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 15:57
 */
@Namespace(COMMON_FUNCTION_SPACE)
public class CommonFunctions {

    /**
     * 获取 Lucky-HttpClient 的版本号
     *
     * @return Lucky-HttpClient 的版本号
     */
    public static String version() {
        return Version.getLuckyHttpClientVersion();
    }


    /**
     * 构建BasicAut格式的字符串
     *
     * @param username 用户名
     * @param password 密码
     * @return BasicAut格式的字符串
     */
    @FunctionAlias("basic_auth")
    public static String basicAuth(String username, String password) throws IOException {
        String auth = "Basic " + username + ":" + password;
        return base64(auth);
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
    @FunctionAlias("time_sec")
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
    @FunctionAlias("format_date")
    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 时间格式化【yyyy-MM-dd HH:mm:ss】
     *
     * @param date 时间
     * @return 格式化后的时间
     */
    @FunctionAlias("yyyy_mm_dd_hh_mm_ss_date")
    public static String yyyyMMddHHmmssDate(Date date) {
        return formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 时间格式化【yyyyMMdd】
     *
     * @param date 时间
     * @return 格式化后的时间
     */
    @FunctionAlias("yyyy_mm_dd_date")
    public static String yyyyMMddDate(Date date) {
        return formatDate(date, "yyyyMMdd");
    }

    /**
     * 格式化当前时间
     *
     * @param format 时间格式
     * @return 格式化后的时间
     */
    @FunctionAlias("format_now")
    public static String formatNow(String format) {
        return formatDate(date(), format);
    }

    /**
     * 格式化当前时间【yyyy-MM-dd HH:mm:ss】
     *
     * @return 格式化后的时间
     */
    @FunctionAlias("yyyy_mm_dd_hh_mm_ss")
    public static String yyyyMMddHHmmss() {
        return yyyyMMddHHmmssDate(date());
    }

    /**
     * 格式化当前时间【yyyyMMdd】
     *
     * @return 格式化后的时间
     */
    @FunctionAlias("yyyy_mm_dd")
    public static String yyyyMMdd() {
        return yyyyMMddDate(date());
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
     *          ``#{lbe('$body$.data')}``
     *      }
     * </pre>
     *
     * @param bodySelect 响应体内容选择表达式
     * @return 将所选内容松散绑定到当前方法上下文方法的返回值上的SpEL表达式
     */
    public static String lbe(String bodySelect) {
        return StringUtils.format("#{lb($mc$, '{}')}", bodySelect);
    }

    /**
     * 检查给定的字符串是否包含实际文本。更具体地说，如果String不为空，其长度大于0，并且至少包含一个非空白字符，则此方法返回true。
     *
     * @param txt 待检测的字符串
     * @return 是否包含实际的文本
     */
    @FunctionAlias("has_text")
    public static boolean hasText(String txt) {
        return StringUtils.hasText(txt);
    }

    /**
     * 检查给定的字符串是否不包含实际文本
     *
     * @param txt 待检测的字符串
     * @return 是否不包含实际的文本
     */
    @FunctionAlias("non_text")
    public static boolean nonText(String txt) {
        return !hasText(txt);
    }

    /**
     * 检查给定的对象是否为null
     *
     * @param obj 待检测的对象
     * @return 是否为null
     */
    @FunctionAlias("is_null")
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 检查给定的对象是否不为null
     *
     * @param obj 待检测的对象
     * @return 是否不为null
     */
    @FunctionAlias("non_null")
    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    /**
     * 如果检测到目标值为null时则返回默认值
     *
     * @param obj 目标值
     * @param def 默认值
     * @param <T> 值类型
     * @return 目标值为null时则返回默认值
     */
    public static <T> T def(T obj, T def) {
        return obj == null ? def : obj;
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
     * URL 拼接
     *
     * @param url  基本 URL
     * @param path Path
     * @return 完整 URL
     */
    @FunctionAlias("join_url")
    public static String joinUrl(String url, String path) {
        return StringUtils.joinUrlPath(url, path);
    }

    /**
     * 字符串拼接
     *
     * @param elements  待拼接的元素
     * @param delimiter 分隔符
     * @return 拼接后的字符串
     */
    public static String join(Object elements, CharSequence delimiter) {
        return StringUtils.join(elements, delimiter);
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
    @FunctionAlias("is_empty")
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
    @FunctionAlias("non_empty")
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
    @FunctionAlias("non_in")
    public static boolean nonIn(Object collection, Object element) {
        return !in(collection, element);
    }

    /**
     * 获取注解实例
     *
     * @param mc             上下文对象
     * @param annotationInfo 注解信息，这里可以是注解实例、注解Class、也可以是注解的全类名
     * @return 注解实例
     * @throws ClassNotFoundException 对应的注解不存在时会抛出该异常
     */
    public static Annotation ann(Context mc, Object annotationInfo) throws ClassNotFoundException {
        return mc.getMergedAnnotation(toAnnotationType(annotationInfo));
    }

    /**
     * 获取注解实例，校验父上下文
     * <pre>
     * cp: Check Parent
     * </pre>
     *
     * @param mc             上下文对象
     * @param annotationInfo 注解信息，这里可以是注解实例、注解Class、也可以是注解的全类名
     * @return 注解实例
     * @throws ClassNotFoundException 对应的注解不存在时会抛出该异常
     */
    public static Annotation anncp(Context mc, Object annotationInfo) throws ClassNotFoundException {
        return mc.getMergedAnnotationCheckParent(toAnnotationType(annotationInfo));
    }

    /**
     * 判断方法上是否存在某个注解
     *
     * @param mc             上下文对象
     * @param annotationInfo 注解信息，这里可以是注解实例、注解Class、也可以是注解的全类名
     * @return 方法上是否存在该注解
     * @throws ClassNotFoundException 对应的注解不存在时会抛出该异常
     */
    @FunctionAlias("has_ann")
    public static boolean hasAnn(Context mc, Object annotationInfo) throws ClassNotFoundException {
        return mc.isAnnotated(toAnnotationType(annotationInfo));
    }

    /**
     * 判断方法上是否存在某个注解，校验父上下文
     * <pre>
     * cp: Check Parent
     * </pre>
     *
     * @param mc             上下文对象
     * @param annotationInfo 注解信息，这里可以是注解实例、注解Class、也可以是注解的全类名
     * @return 方法上是否存在该注解
     * @throws ClassNotFoundException 对应的注解不存在时会抛出该异常
     */
    @FunctionAlias("has_ann_cp")
    public static boolean hasAnncp(Context mc, Object annotationInfo) throws ClassNotFoundException {
        return mc.isAnnotatedCheckParent(toAnnotationType(annotationInfo));
    }

    /**
     * 将响应对象转化为标准Map格式
     *
     * @param response 响应对象
     * @param context  上下文对象
     * @return 标准Map格式
     */
    public static Map<String, Object> sta(Response response, Context context) {
        ConvertMetaData metaData = getConvertMetaType(context);
        String contentType = metaData.getContentType();
        if (StringUtils.hasText(contentType)) {
            response.getHeaderManager().setContentType(contentType);
        }
        Map<String, Object> map = new HashMap<>(16);
        map.put($_RESPONSE_$, LazyValue.of(response));
        map.put($_RESPONSE_STATUS_$, LazyValue.of(response::getStatus));
        map.put($_CONTENT_LENGTH_$, LazyValue.of(response::getResultSize));
        map.put($_CONTENT_TYPE_$, LazyValue.of(response::getContentType));
        map.put($_RESPONSE_HEADER_$, LazyValue.of(response::getSimpleHeaders));
        map.put($_RESPONSE_COOKIE_$, LazyValue.of(response::getSimpleCookies));
        map.put($_RESPONSE_STREAM_BODY_$, LazyValue.rtc(response::getInputStream));
        map.put($_RESPONSE_STRING_BODY_$, LazyValue.of(response::getStringResult));
        map.put($_RESPONSE_BYTE_BODY_$, LazyValue.of(response::getResult));
        map.put($_RESPONSE_BODY_$, LazyValue.of(() -> getResponseBody(response, metaData)));
        return map;
    }

    /**
     * 将某个输入流转化为基于byte数组存储的可重复读输入流
     * <pre>
     *     rrbis: Repeatable Read Byte Input Stream
     * </pre>
     *
     * @param in 原始输入流
     * @return 于byte数组存储的可重复读输入流
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static RepeatableReadByteInputStream rrbis(InputStream in) throws IOException {
        return RepeatableReadStreamUtil.useByteStore(in);
    }

    /**
     * 将某个输入流转化为基于本地文件存储的可重复读输入流
     * <pre>
     *     rrfis: Repeatable Read File Input Stream
     * </pre>
     *
     * @param in 原始输入流
     * @return 基于本地文件存储的可重复读输入流
     * @throws IOException 初始化过程中可能会出现IO异常
     */
    public static RepeatableReadFileInputStream rrfis(InputStream in) throws IOException {
        return RepeatableReadStreamUtil.useFileStore(in);
    }

    /**
     * 构造一个可解析的类型{@link ResolvableType}
     *
     * @param clazzInfo 外层类型（支持的描述类型有：String、Class、）
     * @param generics  泛型类型(支持的描述类型有：String、Class、Type、SerializationTypeToken、ResolvableType)
     * @return 可解析的类型ResolvableType
     */
    @FunctionAlias("type_of")
    public static ResolvableType typeOf(Object clazzInfo, Object... generics) {
        Class<?> clazz = toResolvableType(clazzInfo).resolve();
        ResolvableType[] genericsTypes = new ResolvableType[generics.length];
        for (int i = 0; i < generics.length; i++) {
            genericsTypes[i] = toResolvableType(generics[i]);
        }
        return ResolvableType.forClassWithGenerics(clazz, genericsTypes);
    }

    /**
     * 类型转换，将一个对象转化为另一个类型的对象
     * 底层使用{@link ConversionUtils#conversion(Object, ResolvableType)}实现
     *
     * @param source   原始对象
     * @param typeInfo 目标对象类型(支持的描述类型有：String、Class、Type、SerializationTypeToken、ResolvableType)
     * @return 目标对象
     */
    public static Object convert(Object source, Object typeInfo) {
        return ConversionUtils.conversion(source, toResolvableType(typeInfo));
    }

    /**
     * 对象属性拷贝，基于{@link BeanUtils#copyProperties(Object, Object)} 实现
     *
     * @param source 原对象
     * @param target 目标对象
     * @param <T>    待拷贝的对象类型
     */
    public static <T> void copy(T source, T target) {
        BeanUtils.copyProperties(source, target);
    }

    /**
     * 初始化模式对象属性拷贝，如果target对象中的某个属性不为初始值时（引用类型的初始值为null， 基本类型的初始值参考JDK规范），拷贝时则忽略该属性
     * 基于{@link BeanUtils#copyPropertiesIgnoreNonInitValue(Object, Object)}实现
     *
     * @param source 原对象
     * @param target 目标对象
     * @param <T>    待拷贝的对象类型
     */
    @FunctionAlias("init_copy")
    public static <T> void initCopy(T source, T target) {
        BeanUtils.copyPropertiesIgnoreNonInitValue(source, target);
    }


    @FunctionFilter
    public static ResolvableType toResolvableType(Object clazzInfo) {
        if (clazzInfo instanceof ResolvableType) {
            return (ResolvableType) clazzInfo;
        }
        if (clazzInfo instanceof Class) {
            return ResolvableType.forClass((Class<?>) clazzInfo);
        }
        if (clazzInfo instanceof Type) {
            return ResolvableType.forType((Type) clazzInfo);
        }
        if (clazzInfo instanceof SerializationTypeToken) {
            return ResolvableType.forType(((SerializationTypeToken<?>) clazzInfo).getType());
        }
        if (clazzInfo instanceof String) {
            return ResolvableType.forClass(ClassUtils.getClass((String) clazzInfo));
        }
        throw new IllegalArgumentException("Conversion from '" + ClassUtils.getClassName(clazzInfo) + "' type to 'org.springframework.core.ResolvableType' type is not supported.");
    }

    @FunctionFilter
    public static Charset getCharset(String... charset) {
        return ContainerUtils.isEmptyArray(charset) ? StandardCharsets.UTF_8 : Charset.forName(charset[0]);
    }

    @FunctionFilter
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> toAnnotationType(Object annotationInfo) throws ClassNotFoundException {
        if (annotationInfo instanceof Annotation) {
            return ((Annotation) annotationInfo).annotationType();
        }
        if (annotationInfo instanceof Class) {
            return (Class<? extends Annotation>) annotationInfo;
        }
        if (annotationInfo instanceof String) {
            return (Class<? extends Annotation>) Class.forName((String) annotationInfo);
        }
        throw new IllegalArgumentException("Illegal annotation information: " + annotationInfo);
    }


}
