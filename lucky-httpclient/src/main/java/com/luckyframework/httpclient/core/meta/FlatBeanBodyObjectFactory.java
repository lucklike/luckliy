package com.luckyframework.httpclient.core.meta;

import com.luckyframework.common.FlatBean;
import com.luckyframework.serializable.JDKSerializationScheme;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationScheme;
import org.springframework.lang.NonNull;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.luckyframework.httpclient.core.meta.ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT;
import static com.luckyframework.httpclient.core.meta.ContentType.APPLICATION_JSON;
import static com.luckyframework.httpclient.core.meta.ContentType.JAVA;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * 基于{@link FlatBean}实现的Body对象工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 02:41
 */
public class FlatBeanBodyObjectFactory implements DynamicElementAddBodyObjectFactory {

    private final SerializationScheme serializationScheme;
    private final FlatBean<?> flatBean;
    private final String mimeType;
    private final Charset charset;

    private FlatBeanBodyObjectFactory(SerializationScheme serializationScheme, FlatBean<?> flatBean, String mimeType, Charset charset) {
        this.flatBean = flatBean;
        this.serializationScheme = serializationScheme;
        this.mimeType = mimeType;
        this.charset = charset;
    }

    public static FlatBeanBodyObjectFactory of(SerializationScheme serializationScheme, FlatBean<?> flatBean, String mimeType, Charset charset) {
        return new FlatBeanBodyObjectFactory(serializationScheme, flatBean, mimeType, charset);
    }

    public static FlatBeanBodyObjectFactory initMap(SerializationScheme serializationScheme, String mimeType, Charset charset) {
        return of(serializationScheme, FlatBean.of(new LinkedHashMap<>()), mimeType, charset);
    }

    public static FlatBeanBodyObjectFactory initList(SerializationScheme serializationScheme, String mimeType, Charset charset) {
        return of(serializationScheme, FlatBean.of(new ArrayList<>()), mimeType, charset);
    }

    public static FlatBeanBodyObjectFactory json(FlatBean<?> flatBean) {
        return of(JSON_SCHEME, flatBean, APPLICATION_JSON.getMimeType().toString(), APPLICATION_JSON.getCharset());
    }

    public static FlatBeanBodyObjectFactory jsonMap() {
        return json(FlatBean.of(new LinkedHashMap<>()));
    }

    public static FlatBeanBodyObjectFactory jsonList() {
        return json(FlatBean.of(new ArrayList<>()));
    }

    /**
     * [JSON]
     * 获取请求体参数工厂
     *
     * @param request  请求实例
     * @param flatBean 数据
     * @return 请求体参数工厂
     */
    public static synchronized FlatBeanBodyObjectFactory forJsonRequest(Request request, FlatBean<?> flatBean) {
        BodyObjectFactory bodyFactory = request.getBodyFactory();
        if (!(bodyFactory instanceof FlatBeanBodyObjectFactory)) {
            bodyFactory = FlatBeanBodyObjectFactory.json(flatBean);
            request.setContentType(APPLICATION_JSON);
            request.setBodyFactory(bodyFactory);
        }
        return (FlatBeanBodyObjectFactory) bodyFactory;
    }

    /**
     * [JSON]
     * 获取请求体参数工厂
     *
     * @param request 请求实例
     * @return 请求体参数工厂
     */
    public static synchronized FlatBeanBodyObjectFactory forJsonMapRequest(Request request) {
        return forJsonRequest(request, FlatBean.of(new LinkedHashMap<>()));
    }

    /**
     * [JSON]
     * 获取请求体参数工厂
     *
     * @param request 请求实例
     * @return 请求体参数工厂
     */
    public static synchronized FlatBeanBodyObjectFactory forJsonListRequest(Request request) {
        return forJsonRequest(request, FlatBean.of(new ArrayList<>()));
    }

    public static FlatBeanBodyObjectFactory java(FlatBean<?> flatBean) {
        return of(JDK_SCHEME, flatBean, APPLICATION_JAVA_SERIALIZED_OBJECT.getMimeType().toString(), APPLICATION_JAVA_SERIALIZED_OBJECT.getCharset());
    }

    public static FlatBeanBodyObjectFactory javaMap() {
        return java(FlatBean.of(new LinkedHashMap<>()));
    }

    public static FlatBeanBodyObjectFactory javaList() {
        return java(FlatBean.of(new ArrayList<>()));
    }

    /**
     * [JAVA]
     * 获取请求体参数工厂
     *
     * @param request  请求实例
     * @param flatBean 数据
     * @return 请求体参数工厂
     */
    public static synchronized FlatBeanBodyObjectFactory forJavaRequest(Request request, FlatBean<?> flatBean) {
        BodyObjectFactory bodyFactory = request.getBodyFactory();
        if (!(bodyFactory instanceof FlatBeanBodyObjectFactory)) {
            bodyFactory = FlatBeanBodyObjectFactory.java(flatBean);
            request.setContentType(APPLICATION_JAVA_SERIALIZED_OBJECT);
            request.setBodyFactory(bodyFactory);
        }
        return (FlatBeanBodyObjectFactory) bodyFactory;
    }

    /**
     * [JAVA]
     * 获取请求体参数工厂
     *
     * @param request 请求实例
     * @return 请求体参数工厂
     */
    public static synchronized FlatBeanBodyObjectFactory forJavaMapRequest(Request request) {
        return forJavaRequest(request, FlatBean.of(new LinkedHashMap<>()));
    }

    /**
     * [JAVA]
     * 获取请求体参数工厂
     *
     * @param request 请求实例
     * @return 请求体参数工厂
     */
    public static synchronized FlatBeanBodyObjectFactory forJavaListRequest(Request request) {
        return forJavaRequest(request, FlatBean.of(new ArrayList<>()));
    }

    @NonNull
    @Override
    public BodyObject create() {
        try {
            // Java序列化方案需要特殊处理
            if (serializationScheme instanceof JDKSerializationScheme) {
                return BodyObject.builder(mimeType, charset, ((JDKSerializationScheme) serializationScheme).toByte(flatBean.getBean()), () -> flatBean.getBean().toString());
            }
            return BodyObject.builder(mimeType, charset, serializationScheme.serialization(flatBean.getBean()));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void addElement(String elementName, Object elementValue) {
        flatBean.set(elementName, elementValue);
    }
}
