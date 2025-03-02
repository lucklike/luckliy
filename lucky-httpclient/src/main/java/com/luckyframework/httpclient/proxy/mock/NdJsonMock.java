package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.serializable.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * 模拟application/x-ndjson
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/02/28 16:45
 */
public class NdJsonMock {

    /**
     * Json数据集
     */
    private final List<String> jsonList = new ArrayList<>();

    /**
     * 资源路径
     */
    private String resourceLocation;

    /**
     * 私有构造器
     */
    private NdJsonMock() {

    }

    /**
     * 创建NdJsonMock实例
     *
     * @return NdJsonMock实例
     */
    public static NdJsonMock create() {
        return new NdJsonMock();
    }

    /**
     * 从某个资源中加载NdJson响应模拟数据
     *
     * @param resourceLocation 资源路径
     * @return NdJson响应数据模拟
     */
    public static NdJsonMock resource(String resourceLocation) {
        NdJsonMock sseMock = new NdJsonMock();
        sseMock.resourceLocation = resourceLocation;
        return sseMock;
    }

    /**
     * 构建一个严格控制类型的构建工具
     *
     * @param <T> 实体类型
     * @return 严格控制类型的构建工具
     */
    public static <T> EntityBuilder<T> newBuilder() {
        return new EntityBuilder<>();
    }

    /**
     * 添加一行JSON数据
     *
     * @param json JSON字符串
     * @return 当前对象本身
     */
    public NdJsonMock addLine(String json) {
        jsonList.add(json);
        return this;
    }

    /**
     * 添加一行JSON数据
     *
     * @param jsonObject 对象
     * @return 当前对象本身
     */
    public NdJsonMock addLine(Object jsonObject) {
        try {
            return addLine(JSON_SCHEME.serialization(jsonObject));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 获取文本流
     *
     * @return 文本流
     */
    public InputStream getTxtStream() {
        if (resourceLocation != null) {
            try {
                return CommonFunctions.resource(resourceLocation).getInputStream();
            } catch (IOException e) {
                throw new LuckyIOException(e);
            }
        }
        return new ByteArrayInputStream(getData().getBytes());
    }

    /**
     * 获取最终数据
     *
     * @return 最终数据
     */
    public String getData() {
        StringBuilder dataSb = new StringBuilder();
        for (String json : jsonList) {
            dataSb.append(json).append("\n");
        }
        return dataSb.toString();
    }


    @Override
    public String toString() {
        if (resourceLocation != null) {
            return "[Resource]: " + resourceLocation;
        }
        return getData();
    }

    /**
     * 严格控制类型的构建工具
     *
     * @param <T> 实体类型
     */
    public static class EntityBuilder<T> {
        private final NdJsonMock ndJsonMock;

        EntityBuilder() {
            ndJsonMock = new NdJsonMock();
        }

        public EntityBuilder<T> addLine(T entry) {
            ndJsonMock.addLine(entry);
            return this;
        }

        public NdJsonMock build() {
            return ndJsonMock;
        }
    }
}
