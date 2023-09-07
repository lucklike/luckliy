package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import com.luckyframework.httpclient.core.impl.DefaultResponse;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Http请求执行器
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:22 下午
 */
@FunctionalInterface
public interface HttpExecutor {

    Logger logger = LoggerFactory.getLogger(HttpExecutor.class);

    /**
     * 执行http请求
     *
     * @param request   请求实例
     * @param processor 响应处理器
     */
    void doExecute(Request request, ResponseProcessor processor) throws Exception;

    /**
     * 执行http请求
     *
     * @param request   请求实例
     * @param processor 响应处理器
     */
    default void execute(Request request, ResponseProcessor processor) {
        if (request instanceof DefaultRequest) {
            ((DefaultRequest) request).init();
        }
        try {
            doExecute(request, processor);
        } catch (Exception e) {
            processor.exceptionHandler(request, e);
        }

    }

    /**
     * 执行请求得到响应
     *
     * @param request   请求
     * @param processor 响应处理器
     * @return 响应
     */
    default Response execute(Request request, SaveResultResponseProcessor processor) {
        execute(request, (ResponseProcessor) processor);
        return processor.getResponse();
    }

    /**
     * 执行请求得到响应
     *
     * @param request 请求
     * @return 响应
     */
    default Response execute(Request request) {
        return execute(request, DefaultResponse.getCommonProcessor());
    }


    /**
     * 执行请求得到{@link String}类型响应
     *
     * @param request 请求
     * @return 响应
     */
    default String getString(Request request) {
        return execute(request).getStringResult();
    }

    /**
     * 执行请求得到{@link byte[]}类型响应
     *
     * @param request 请求
     * @return 响应
     */
    default byte[] getBytes(Request request) {
        return execute(request).getResult();
    }

    /**
     * 执行请求得到{@link InputStream}类型响应
     *
     * @param request 请求
     * @return 响应
     */
    default InputStream getInputStream(Request request) {
        return execute(request).getInputStream();
    }

    /**
     * 执行请求得到{@link InputStreamSource}类型响应
     *
     * @param request 请求
     * @return 响应
     */
    default InputStreamSource getInputStreamSource(Request request) {
        return execute(request).getInputStreamSource();
    }

    /**
     * 执行请求得到{@link MultipartFile}类型响应
     *
     * @param request 请求
     * @return 响应
     */
    default MultipartFile getMultipartFile(Request request) {
        return execute(request).getMultipartFile();
    }

    /**
     * 执行请求得到{@link T }类型的响应实体
     *
     * @param request 请求
     * @param tClass  实体的Class
     * @return 响应
     */
    default <T> T getEntity(Request request, Class<T> tClass) {
        return execute(request).getEntity(tClass);
    }

    /**
     * 执行请求得到{@link T }类型的响应实体
     *
     * @param request   请求
     * @param typeToken 实体的泛型Token
     * @return 响应
     */
    default <T> T getEntity(Request request, SerializationTypeToken<T> typeToken) {
        return execute(request).getEntity(typeToken);
    }

    /**
     * 执行请求得到{@link T }类型的响应实体
     *
     * @param request 请求
     * @param type    泛型
     * @return 响应
     */
    default <T> T getEntity(Request request, Type type) {
        return execute(request).getEntity(type);
    }

    /**
     * 执行请求得到{@link Map }类型结果
     *
     * @param request 请求
     * @return Map类型结果
     */
    default Map<String, Object> getMap(Request request) {
        return execute(request).getMapResult();
    }

    /**
     * 执行请求得到List&lt;Map&lt;String, Object>>类型结果
     *
     * @param request 请求
     * @return List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> getMapList(Request request) {
        return execute(request).getMapListResult();
    }

    /**
     * 执行请求得到{@link ConfigurationMap }类型结果
     *
     * @param request 请求
     * @return ConfigurationMap类型结果
     */
    default ConfigurationMap getConfigMap(Request request) {
        return execute(request).getConfigMapResult();
    }

    /**
     * 执行请求得到List&lt;ConfigurationMap>类型结果
     *
     * @param request 请求
     * @return List&lt;ConfigurationMap>类型结果
     */
    default List<ConfigurationMap> getConfigMapList(Request request) {
        return execute(request).getConfigMapListResult();
    }


    //-------------------------------------------------------------
    //                    [GET] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[GET]请求，没有返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     */
    default void get(String url, Object... urlParams) {
        execute(Request.get(url, urlParams), ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[GET]请求，返回{@link String}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return String类型结果
     */
    default String getForString(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getStringResult();
    }

    /**
     * 发起一个[GET]请求，返回{@link byte byte[]}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return byte[]类型结果
     */
    default byte[] getForBytes(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getResult();
    }

    /**
     * 发起一个[GET]请求，返回{@link InputStream}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return InputStream类型结果
     */
    default InputStream getForInputStream(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getInputStream();
    }

    /**
     * 发起一个[GET]请求，返回{@link InputStreamSource}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return InputStreamSource类型结果
     */
    default InputStreamSource getForInputStreamSource(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getInputStreamSource();
    }

    /**
     * 发起一个[GET]请求，返回{@link MultipartFile}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return MultipartFile类型结果
     */
    default MultipartFile getForMultipartFile(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getMultipartFile();
    }

    /**
     * 发起一个[GET]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param tClass    实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @return tClass类型结果
     */
    default <T> T getForEntity(String url, Class<T> tClass, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getEntity(tClass);
    }

    /**
     * 发起一个[GET]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @return typeToken类型结果
     */
    default <T> T getForEntity(String url, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getEntity(typeToken);
    }

    /**
     * 发起一个[GET]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param type      实体Type
     * @param urlParams Rest参数占位符的填充值
     * @return type类型结果
     */
    default <T> T getForEntity(String url, Type type, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getEntity(type);
    }

    /**
     * 执行[GET]请求得到{@link Map }类型结果
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return Map类型结果
     */
    default Map<String, Object> getForMap(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getMapResult();
    }

    /**
     * 执行[GET]请求得到List&lt;Map&lt;String, Object>>类型结果
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> getForMapList(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getMapListResult();
    }

    /**
     * 执行[GET]请求得到{@link ConfigurationMap }类型结果
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return ConfigurationMap类型结果
     */
    default ConfigurationMap getForConfigMap(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getConfigMapResult();
    }

    /**
     * 执行[GET]请求得到List&lt;ConfigurationMap>类型结果
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return List&lt;ConfigurationMap>类型结果
     */
    default List<ConfigurationMap> getForConfigMapList(String url, Object... urlParams) {
        return execute(Request.get(url, urlParams)).getConfigMapListResult();
    }


    //-------------------------------------------------------------
    //                    [POST] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[POST]请求，没有返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     */
    default void post(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        execute(Request.post(url, urlParams).setRequestParameter(requestParamMap),
                ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[POST]请求，返回{@link String }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String postForString(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getStringResult();
    }

    /**
     * 发起一个[POST]请求，返回{@link byte byte[] }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] postForBytes(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getResult();
    }

    /**
     * 发起一个[POST]请求，返回{@link InputStream }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream postForInputStream(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getInputStream();
    }

    /**
     * 发起一个[POST]请求，返回{@link InputStreamSource }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource postForInputStreamSource(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getInputStreamSource();
    }

    /**
     * 发起一个[POST]请求，返回{@link MultipartFile }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile postForMultipartFile(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getMultipartFile();
    }

    /**
     * 发起一个[POST]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param tClass          实体Class
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T postForEntity(String url, Map<String, Object> requestParamMap, Class<T> tClass, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getEntity(tClass);
    }

    /**
     * 发起一个[POST]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param typeToken       实体TypeToken
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T postForEntity(String url, Map<String, Object> requestParamMap, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getEntity(typeToken);
    }

    /**
     * 发起一个[POST]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param type            实体Type
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T postForEntity(String url, Map<String, Object> requestParamMap, Type type, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getEntity(type);
    }

    /**
     * 发起一个[POST]请求，返回{@link Map }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> postForMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getMapResult();
    }

    /**
     * 发起一个[POST]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> postForMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getMapListResult();
    }

    /**
     * 发起一个[POST]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap postForConfigMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapResult();
    }

    /**
     * 发起一个[POST]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> postForConfigMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.post(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapListResult();
    }


    //-------------------------------------------------------------
    //                  [DELETE] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[DELETE]请求，没有返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     */
    default void delete(String url, Object... urlParams) {
        execute(Request.delete(url, urlParams), ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[DELETE]请求，返回{@link String}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String deleteForString(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getStringResult();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link byte byte[]}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] deleteForBytes(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getResult();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link InputStream}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream deleteForInputStream(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getInputStream();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link InputStreamSource}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource deleteForInputStreamSource(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getInputStreamSource();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link MultipartFile}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile deleteForMultipartFile(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getMultipartFile();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param tClass    实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T deleteForEntity(String url, Class<T> tClass, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getEntity(tClass);
    }

    /**
     * 发起一个[DELETE]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T deleteForEntity(String url, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getEntity(typeToken);
    }

    /**
     * 发起一个[DELETE]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param type      实体Type
     * @param urlParams Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T deleteForEntity(String url, Type type, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getEntity(type);
    }

    /**
     * 发起一个[DELETE]请求，返回{@link Map }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> deleteForMap(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getMapResult();
    }

    /**
     * 发起一个[DELETE]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> deleteForMapList(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getMapListResult();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap deleteForConfigMap(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getConfigMapResult();
    }

    /**
     * 发起一个[DELETE]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> deleteForConfigMapList(String url, Object... urlParams) {
        return execute(Request.delete(url, urlParams)).getConfigMapListResult();
    }


    //-------------------------------------------------------------
    //                  [PUT] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[PUT]请求，没有返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     */
    default void put(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        execute(request, ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[PUT]请求，返回{@link String }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String putForString(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getStringResult();
    }

    /**
     * 发起一个[PUT]请求，返回{@link byte byte[] }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] putForBytes(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getResult();
    }

    /**
     * 发起一个[PUT]请求，返回{@link InputStream }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream putForInputStream(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getInputStream();
    }

    /**
     * 发起一个[PUT]请求，返回{@link InputStreamSource }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource putForInputStreamSource(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getInputStreamSource();
    }

    /**
     * 发起一个[PUT]请求，返回{@link MultipartFile }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile putForMultipartFile(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getMultipartFile();
    }

    /**
     * 发起一个[PUT]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param tClass          实体Class
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T putForEntity(String url, Map<String, Object> requestParamMap, Class<T> tClass, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getEntity(tClass);
    }

    /**
     * 发起一个[PUT]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param typeToken       实体TypeToken
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T putForEntity(String url, Map<String, Object> requestParamMap, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getEntity(typeToken);
    }

    /**
     * 发起一个[PUT]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param type            实体Type
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T putForEntity(String url, Map<String, Object> requestParamMap, Type type, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getEntity(type);
    }

    /**
     * 发起一个[PUT]请求，返回{@link Map }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> putForMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getMapResult();
    }

    /**
     * 发起一个[PUT]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> putForMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getMapListResult();
    }

    /**
     * 发起一个[PUT]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap putForConfigMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapResult();
    }

    /**
     * 发起一个[PUT]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> putForConfigMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.put(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapListResult();
    }


    //-------------------------------------------------------------
    //                  [HEAD] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[HEAD]请求，没有返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     */
    default void head(String url, Object... urlParams) {
        execute(Request.head(url, urlParams), ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[HEAD]请求，返回{@link String}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String headForString(String url, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getStringResult();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link byte byte[]}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] headForBytes(String url, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getResult();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link InputStream}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream headForInputStream(String url, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getInputStream();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link InputStreamSource}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource headForInputStreamSource(String url, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getInputStreamSource();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link MultipartFile}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile headForMultipartFile(String url, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getMultipartFile();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param tClass    实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T headForEntity(String url, Class<T> tClass, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getEntity(tClass);
    }

    /**
     * 发起一个[HEAD]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T headForEntity(String url, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getEntity(typeToken);
    }

    /**
     * 发起一个[HEAD]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param type      实体Type
     * @param urlParams Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T headForEntity(String url, Type type, Object... urlParams) {
        return execute(Request.head(url, urlParams)).getEntity(type);
    }

    /**
     * 发起一个[HEAD]请求，返回{@link Map }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> headForMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.head(url, urlParams).setRequestParameter(requestParamMap)).getMapResult();
    }

    /**
     * 发起一个[HEAD]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> headForMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.head(url, urlParams).setRequestParameter(requestParamMap)).getMapListResult();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap headForConfigMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.head(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapResult();
    }

    /**
     * 发起一个[HEAD]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> headForConfigMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.head(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapListResult();
    }

    //-------------------------------------------------------------
    //                  [PATCH] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[PATCH]请求，没有返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     */
    default void patch(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap), ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[PATCH]请求，返回{@link String}类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String patchForString(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getStringResult();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link byte byte[]}类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] patchForBytes(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getResult();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link InputStream}类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream patchForInputStream(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getInputStream();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link InputStreamSource}类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource patchForInputStreamSource(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getInputStreamSource();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link MultipartFile }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile patchForMultipartFile(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param tClass          实体的Class
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T patchForEntity(String url, Map<String, Object> requestParamMap, Class<T> tClass, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getEntity(tClass);
    }

    /**
     * 发起一个[PATCH]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param typeToken       实体TypeToken
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T patchForEntity(String url, Map<String, Object> requestParamMap, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getEntity(typeToken);
    }

    /**
     * 发起一个[PATCH]请求，返回{@link T }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param type            实体Type
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T patchForEntity(String url, Map<String, Object> requestParamMap, Type type, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getEntity(type);
    }

    /**
     * 发起一个[PATCH]请求，返回{@link Map }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> patchForMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getMapResult();
    }

    /**
     * 发起一个[PATCH]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> patchForMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getMapListResult();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap patchForConfigMap(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapResult();
    }

    /**
     * 发起一个[PATCH]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url             URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams       Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> patchForConfigMapList(String url, Map<String, Object> requestParamMap, Object... urlParams) {
        return execute(Request.patch(url, urlParams).setRequestParameter(requestParamMap)).getConfigMapListResult();
    }

    //-------------------------------------------------------------
    //                  [CONNECT] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[CONNECT]请求，没有返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     */
    default void connect(String url, Object... urlParams) {
        execute(Request.connect(url, urlParams), ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link String}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String connectForString(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getStringResult();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link byte byte[]}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] connectForBytes(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getResult();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link InputStream}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream connectForInputStream(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getInputStream();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link InputStreamSource}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource connectForInputStreamSource(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getInputStreamSource();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link MultipartFile}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile connectForMultipartFile(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getMultipartFile();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param tClass    实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T connectForEntity(String url, Class<T> tClass, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getEntity(tClass);
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T connectForEntity(String url, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getEntity(typeToken);
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param type      实体Type
     * @param urlParams Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T connectForEntity(String url, Type type, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getEntity(type);
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link Map }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> connectForMap(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getMapResult();
    }

    /**
     * 发起一个[CONNECT]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> connectForMapList(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getMapListResult();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap connectForConfigMap(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getConfigMapResult();
    }

    /**
     * 发起一个[CONNECT]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> connectForConfigMapList(String url, Object... urlParams) {
        return execute(Request.connect(url, urlParams)).getConfigMapListResult();
    }

    //-------------------------------------------------------------
    //                  [OPTIONS] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[OPTIONS]请求，没有返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     */
    default void options(String url, Object... urlParams) {
        execute(Request.options(url, urlParams), ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link String}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String optionsForString(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getStringResult();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link byte byte[]}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] optionsForBytes(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getResult();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link InputStream}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream optionsForInputStream(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getInputStream();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link InputStreamSource}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource optionsForInputStreamSource(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getInputStreamSource();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link MultipartFile}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile optionsForMultipartFile(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getMultipartFile();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param tClass    实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T optionsForEntity(String url, Class<T> tClass, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getEntity(tClass);
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T optionsForEntity(String url, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getEntity(typeToken);
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param type      实体Type
     * @param urlParams Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T optionsForEntity(String url, Type type, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getEntity(type);
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link Map }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> optionsForMap(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getMapResult();
    }

    /**
     * 发起一个[OPTIONS]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> optionsForMapList(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getMapListResult();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap optionsForConfigMap(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getConfigMapResult();
    }

    /**
     * 发起一个[OPTIONS]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> optionsForConfigMapList(String url, Object... urlParams) {
        return execute(Request.options(url, urlParams)).getConfigMapListResult();
    }


    //-------------------------------------------------------------
    //                  [TRACE] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[TRACE]请求，没有返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     */
    default void trace(String url, Object... urlParams) {
        execute(Request.trace(url, urlParams), ResponseProcessor.DO_NOTHING_PROCESSOR);
    }

    /**
     * 发起一个[TRACE]请求，返回{@link String}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回String类型结果
     */
    default String traceForString(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getStringResult();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link byte byte[]}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回byte[]类型结果
     */
    default byte[] traceForBytes(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getResult();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link InputStream}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStream类型结果
     */
    default InputStream traceForInputStream(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getInputStream();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link InputStreamSource}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回InputStreamSource类型结果
     */
    default InputStreamSource traceForInputStreamSource(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getInputStreamSource();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link MultipartFile}类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回MultipartFile类型结果
     */
    default MultipartFile traceForMultipartFile(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getMultipartFile();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param tClass    实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @return 返回tClass类型结果
     */
    default <T> T traceForEntity(String url, Class<T> tClass, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getEntity(tClass);
    }

    /**
     * 发起一个[TRACE]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @return 返回typeToken类型结果
     */
    default <T> T traceForEntity(String url, SerializationTypeToken<T> typeToken, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getEntity(typeToken);
    }

    /**
     * 发起一个[TRACE]请求，返回{@link T }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param type      实体Type
     * @param urlParams Rest参数占位符的填充值
     * @return 返回type类型结果
     */
    default <T> T traceForEntity(String url, Type type, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getEntity(type);
    }

    /**
     * 发起一个[TRACE]请求，返回{@link Map }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default Map<String, Object> traceForMap(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getMapResult();
    }

    /**
     * 发起一个[TRACE]请求，返回List&lt;Map&lt;String, Object>>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> traceForMapList(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getMapListResult();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link ConfigurationMap }类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回Map类型结果
     */
    default ConfigurationMap traceForConfigMap(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getConfigMapResult();
    }

    /**
     * 发起一个[TRACE]请求，返回List&lt;ConfigurationMap>类型的返回值
     *
     * @param url       URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @return 返回List&lt;Map&lt;String, Object>>类型结果
     */
    default List<ConfigurationMap> traceForConfigMapList(String url, Object... urlParams) {
        return execute(Request.trace(url, urlParams)).getConfigMapListResult();
    }

    //-------------------------------------------------------------
    //                  static Methods
    //-------------------------------------------------------------

    /**
     * 判断是否为文件类型的请求
     *
     * @param params 参数列表
     * @return 是否为文件类型的请求
     */
    static boolean isFileRequest(Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (isResourceParam(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断某个对象是否为资源类型参数
     *
     * @param param 待检验参数
     * @return 是否为资源类型参数
     */
    static boolean isResourceParam(Object param) {
        if (param == null) {
            return false;
        }
        Class<?> elementType = ContainerUtils.getElementType(param);
        return File.class.isAssignableFrom(elementType) ||
                Resource.class.isAssignableFrom(elementType) ||
                MultipartFile.class.isAssignableFrom(elementType) ||
                HttpFile.class.isAssignableFrom(elementType);
    }

    /**
     * 判断某个类型是否为资源类型参数
     *
     * @param paramType 待检验类型
     * @return 是否为资源类型参数
     */
    static boolean isResourceParam(ResolvableType paramType) {
        Class<?> elementType = ContainerUtils.getElementType(paramType);
        return File.class.isAssignableFrom(elementType) ||
                Resource.class.isAssignableFrom(elementType) ||
                MultipartFile.class.isAssignableFrom(elementType) ||
                HttpFile.class.isAssignableFrom(elementType);
    }

    /**
     * 将参数转化为{@link HttpFile}类型参数
     *
     * @param param 待转换参数
     * @return HttpFile类型参数
     */
    static HttpFile toHttpFile(Object param) {
        if (param instanceof HttpFile) {
            return (HttpFile) param;
        }
        if (param instanceof File) {
            return new HttpFile((File) param);
        }
        if (param instanceof Resource) {
            return new HttpFile((Resource) param);
        }
        if (param instanceof MultipartFile) {
            return new HttpFile((MultipartFile) param);
        }
        throw new IllegalArgumentException("Unable to convert '" + param + "' to '" + HttpFile.class + "'");
    }

    /**
     * 将参数转化为{@link HttpFile[] }类型参数
     *
     * @param params 待转换参数
     * @return HttpFile[]类型参数
     */
    static HttpFile[] toHttpFiles(Object params) {
        if (ContainerUtils.isIterable(params)) {
            List<HttpFile> httpFileList = new ArrayList<>();
            Iterator<Object> iterator = ContainerUtils.getIterator(params);
            while (iterator.hasNext()) {
                httpFileList.add(toHttpFile(iterator.next()));
            }
            return httpFileList.toArray(new HttpFile[0]);
        } else {
            HttpFile[] httpFiles = new HttpFile[1];
            httpFiles[0] = toHttpFile(params);
            return httpFiles;
        }

    }
}
