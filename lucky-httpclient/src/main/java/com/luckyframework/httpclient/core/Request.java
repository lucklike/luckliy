package com.luckyframework.httpclient.core;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.impl.DefaultRequest;

/**
 * 请求接口
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:21 下午
 */
public interface Request extends RequestParameter, HttpHeaderManager {

    /**
     * 默认的连接超时时间
     */
    Integer DEF_CONNECTION_TIME_OUT = 60 * 1000;
    /**
     * 默认的读超时时间
     */
    Integer DEF_READ_TIME_OUT = 20 * 1000;
    /**
     * 默认的写超时时间
     */
    Integer DEF_WRITER_TIME_OUT = 20 * 1000;

    /**
     * 目标资源的完整URL地址
     */
    String getUrl();

    /**
     * 请求方式(GET、POST、DELETE、PUT...)
     */
    RequestMethod getRequestMethod();

    /**
     * 请求头管理器
     */
    HttpHeaderManager getHeaderManager();

    /**
     * 请求参数
     */
    RequestParameter getRequestParameter();

    /**
     * 获取连接超时时间
     *
     * @return
     */
    Integer getConnectTimeout();

    /**
     * 设置连接超时时间(单位：ms；默认值: 5000ms)
     *
     * @param connectionTime 连接超时时间 ,单位：ms
     */
    void setConnectTimeout(Integer connectionTime);

    /**
     * 获取数据读取的超时时间
     *
     * @return
     */
    Integer getReadTimeout();

    /**
     * 设置数据读取的超时时间(单位：ms；默认值: 5000ms)
     *
     * @param readTimeout 响应数据的超时时间,单位：ms
     */
    void setReadTimeout(Integer readTimeout);

    /**
     * 获取数据写入的超时时间
     *
     * @return
     */
    Integer getWriterTimeout();

    /**
     * 设置数据写入的超时时间(单位：ms；默认值: 5000ms)
     *
     * @param writerTimeout 响应数据的超时时间,单位：ms
     */
    void setWriterTimeout(Integer writerTimeout);


    //-------------------------------------------------------------------
    //              Static Methods (Builder Method)
    //-------------------------------------------------------------------

    /**
     * 快速构建一个Http请求
     *
     * @param url           请求地址,支持Rest参数占位符
     * @param requestMethod 请求方式[GET、POST、DELETE、PUT...]
     * @param pathParams        Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request builder(String url, RequestMethod requestMethod, Object... pathParams) {
        url = StringUtils.format(url, pathParams);
        return new DefaultRequest(url, requestMethod);
    }

    /**
     * 构建一个Http[GET]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request get(String url, Object... pathParams) {
        return builder(url, RequestMethod.GET, pathParams);
    }

    /**
     * 构建一个Http[POST]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request post(String url, Object... pathParams) {
        return builder(url, RequestMethod.POST, pathParams);
    }

    /**
     * 构建一个Http[DELETE]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request delete(String url, Object... pathParams) {
        return builder(url, RequestMethod.DELETE, pathParams);
    }

    /**
     * 构建一个Http[PUT]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request put(String url, Object... pathParams) {
        return builder(url, RequestMethod.PUT, pathParams);
    }

    /**
     * 构建一个Http[HEAD]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request head(String url, Object... pathParams) {
        return builder(url, RequestMethod.HEAD, pathParams);
    }

    /**
     * 构建一个Http[PATCH]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request patch(String url, Object... pathParams) {
        return builder(url, RequestMethod.PATCH, pathParams);
    }

    /**
     * 构建一个Http[CONNECT]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request connect(String url, Object... pathParams) {
        return builder(url, RequestMethod.CONNECT, pathParams);
    }

    /**
     * 构建一个Http[OPTIONS]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request options(String url, Object... pathParams) {
        return builder(url, RequestMethod.OPTIONS, pathParams);
    }

    /**
     * 构建一个Http[TRACE]请求
     *
     * @param url    请求地址,支持Rest参数占位符
     * @param pathParams Rest参数占位符的填充值
     * @return {@link DefaultRequest}
     */
    static Request trace(String url, Object... pathParams) {
        return builder(url, RequestMethod.TRACE, pathParams);
    }

}
