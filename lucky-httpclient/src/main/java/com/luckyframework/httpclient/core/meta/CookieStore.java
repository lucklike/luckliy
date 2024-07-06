package com.luckyframework.httpclient.core.meta;

/**
 * Cookie存储库接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/9 08:23
 */
public interface CookieStore {

    /**
     * 加载Cookie到请求中
     *
     * @param request 请求实例
     */
    void loadCookie(Request request);

    /**
     * 保存Cookie
     *
     * @param responseMetaData 响应元数据
     */
    void saveCookie(ResponseMetaData responseMetaData);

}
