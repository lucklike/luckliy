package com.luckyframework.httpclient.core.executor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.impl.DefaultResponse;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Http请求执行器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:22 下午
 */
public interface HttpExecutor {

    Logger logger = LoggerFactory.getLogger(HttpExecutor.class);

    void execute(Request request,ResponseProcessor processor) throws IOException;

    /**
     * 执行请求得到响应
     * @param request 请求
     * @return 响应
     */
    default Response execute(Request request) throws IOException{
        DefaultResponse response = new DefaultResponse();
        execute(request,(status, header, result) -> {
            response.setState(status);
            response.setHeaderManager(header);
            response.setResult(FileCopyUtils.copyToByteArray(result));
        });
        if(200 != response.getState()){
            logger.warn("For the {} request, the response status code of the server is:{},context is:{}",request,response.getState(),response.getStringResult());
        }
        return response;
    }


    /**
     * 执行请求得到{@link String}类型响应
     * @param request 请求
     * @return 响应
     * @throws IOException
     */
    default String getString(Request request)throws IOException{
        return execute(request).getStringResult();
    }

    /**
     * 执行请求得到{@link byte[]}类型响应
     * @param request 请求
     * @return 响应
     * @throws IOException
     */
    default byte[] getBytes(Request request)throws IOException{
        return execute(request).getResult();
    }

    /**
     * 执行请求得到{@link InputStream}类型响应
     * @param request 请求
     * @return 响应
     * @throws IOException
     */
    default InputStream getInputStream(Request request)throws IOException{
        return execute(request).getInputStream();
    }

    /**
     * 执行请求得到{@link InputStreamSource}类型响应
     * @param request 请求
     * @return 响应
     * @throws IOException
     */
    default InputStreamSource getInputStreamSource(Request request) throws IOException {
        return execute(request).getInputStreamSource();
    }

    /**
     * 执行请求得到{@link MultipartFile}类型响应
     * @param request 请求
     * @return 响应
     * @throws IOException
     */
    default MultipartFile getMultipartFile(Request request) throws IOException {
        return execute(request).getMultipartFile();
    }

    /**
     * 执行请求得到{@link T }类型的响应实体
     * @param request 请求
     * @param tClass  实体的Class
     * @return 响应
     * @throws IOException
     */
    default <T> T getEntity(Request request,Class<T> tClass)throws IOException{
        return execute(request).getEntity(tClass);
    }

    /**
     * 执行请求得到{@link T }类型的响应实体
     * @param request 请求
     * @param typeToken  实体的泛型Token
     * @return 响应
     * @throws IOException
     */
    default <T> T getEntity(Request request, SerializationTypeToken<T> typeToken)throws IOException{
        return execute(request).getEntity(typeToken);
    }

    /**
     * 执行请求得到{@link T }类型的响应实体
     * @param request 请求
     * @param type  泛型
     * @return 响应
     * @throws IOException
     */
    default <T> T getEntity(Request request, Type type)throws IOException{
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                    [GET] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[GET]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void get(String url,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        execute(request);
    }

    /**
     * 发起一个[GET]请求，返回{@link String}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String getForString(String url,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[GET]请求，返回{@link byte byte[]}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] getForBytes(String url,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getResult();
    }

    /**
     * 发起一个[GET]请求，返回{@link InputStream}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream getForInputStream(String url,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[GET]请求，返回{@link InputStreamSource}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource getForInputStreamSource(String url,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[GET]请求，返回{@link MultipartFile}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile getForMultipartFile(String url,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[GET]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param tClass 实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T getForEntity(String url,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[GET]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T getForEntity(String url,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[GET]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T getForEntity(String url,Type type,Object...urlParams) throws IOException {
        Request request = Request.get(url, urlParams);
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                    [POST] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[POST]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void post(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        execute(request);
    }

    /**
     * 发起一个[POST]请求，返回{@link String }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String postForString(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[POST]请求，返回{@link byte byte[] }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] postForBytes(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getResult();
    }

    /**
     * 发起一个[POST]请求，返回{@link InputStream }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream postForInputStream(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[POST]请求，返回{@link InputStreamSource }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource postForInputStreamSource(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[POST]请求，返回{@link MultipartFile }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile postForMultipartFile(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[POST]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param tClass 实体Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T postForEntity(String url,Map<String,Object> requestParamMap,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[POST]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T postForEntity(String url,Map<String,Object> requestParamMap,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[POST]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T postForEntity(String url,Map<String,Object> requestParamMap,Type type,Object...urlParams) throws IOException {
        Request request = Request.post(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                  [DELETE] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[DELETE]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void delete(String url,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        execute(request);
    }

    /**
     * 发起一个[DELETE]请求，返回{@link String}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String deleteForString(String url,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link byte byte[]}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] deleteForBytes(String url,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getResult();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link InputStream}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream deleteForInputStream(String url,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link InputStreamSource}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource deleteForInputStreamSource(String url,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link MultipartFile}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile deleteForMultipartFile(String url,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[DELETE]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param tClass 实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T deleteForEntity(String url,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[DELETE]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T deleteForEntity(String url,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[DELETE]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T deleteForEntity(String url,Type type,Object...urlParams) throws IOException {
        Request request = Request.delete(url, urlParams);
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                  [PUT] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[PUT]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void put(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        execute(request);
    }

    /**
     * 发起一个[PUT]请求，返回{@link String }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String putForString(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[PUT]请求，返回{@link byte byte[] }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] putForBytes(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getResult();
    }

    /**
     * 发起一个[PUT]请求，返回{@link InputStream }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream putForInputStream(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[PUT]请求，返回{@link InputStreamSource }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource putForInputStreamSource(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[PUT]请求，返回{@link MultipartFile }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile putForMultipartFile(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[PUT]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param tClass 实体Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T putForEntity(String url,Map<String,Object> requestParamMap,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[PUT]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T putForEntity(String url,Map<String,Object> requestParamMap,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[PUT]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T putForEntity(String url,Map<String,Object> requestParamMap,Type type,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                  [HEAD] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[HEAD]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void head(String url,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        execute(request);
    }

    /**
     * 发起一个[HEAD]请求，返回{@link String}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String headForString(String url,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link byte byte[]}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] headForBytes(String url,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getResult();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link InputStream}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream headForInputStream(String url,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link InputStreamSource}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource headForInputStreamSource(String url,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link MultipartFile}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile headForMultipartFile(String url,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[HEAD]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param tClass 实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T headForEntity(String url,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[HEAD]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T headForEntity(String url,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[HEAD]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T headForEntity(String url,Type type,Object...urlParams) throws IOException {
        Request request = Request.head(url, urlParams);
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                  [PATCH] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[PATCH]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void patch(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.put(url, urlParams);
        request.setRequestParameter(requestParamMap);
        execute(request);
    }

    /**
     * 发起一个[PATCH]请求，返回{@link String}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String patchForString(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link byte byte[]}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] patchForBytes(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getResult();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link InputStream}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream patchForInputStream(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link InputStreamSource}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource patchForInputStreamSource(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link MultipartFile }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile patchForMultipartFile(String url,Map<String,Object> requestParamMap,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[PATCH]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param tClass 实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T patchForEntity(String url,Map<String,Object> requestParamMap,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[PATCH]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T patchForEntity(String url,Map<String,Object> requestParamMap,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[PATCH]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param requestParamMap 请求参数
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T patchForEntity(String url,Map<String,Object> requestParamMap,Type type,Object...urlParams) throws IOException {
        Request request = Request.patch(url, urlParams);
        request.setRequestParameter(requestParamMap);
        return execute(request).getEntity(type);
    }

    //-------------------------------------------------------------
    //                  [CONNECT] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[CONNECT]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void connect(String url,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        execute(request);
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link String}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String connectForString(String url,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link byte byte[]}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] connectForBytes(String url,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getResult();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link InputStream}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream connectForInputStream(String url,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link InputStreamSource}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource connectForInputStreamSource(String url,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link MultipartFile}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile connectForMultipartFile(String url,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param tClass 实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T connectForEntity(String url,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T connectForEntity(String url,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[CONNECT]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T connectForEntity(String url,Type type,Object...urlParams) throws IOException {
        Request request = Request.connect(url, urlParams);
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                  [OPTIONS] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[OPTIONS]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void options(String url,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        execute(request);
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link String}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String optionsForString(String url,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link byte byte[]}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] optionsForBytes(String url,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getResult();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link InputStream}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream optionsForInputStream(String url,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link InputStreamSource}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource optionsForInputStreamSource(String url,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link MultipartFile}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile optionsForMultipartFile(String url,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param tClass 实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T optionsForEntity(String url,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T optionsForEntity(String url,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[OPTIONS]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T optionsForEntity(String url,Type type,Object...urlParams) throws IOException {
        Request request = Request.options(url, urlParams);
        return execute(request).getEntity(type);
    }


    //-------------------------------------------------------------
    //                  [TRACE] Methods
    //-------------------------------------------------------------

    /**
     * 发起一个[TRACE]请求，没有返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default void trace(String url,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        execute(request);
    }

    /**
     * 发起一个[TRACE]请求，返回{@link String}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default String traceForString(String url,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getStringResult();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link byte byte[]}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default byte[] traceForBytes(String url,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getResult();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link InputStream}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStream traceForInputStream(String url,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getInputStream();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link InputStreamSource}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default InputStreamSource traceForInputStreamSource(String url,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getInputStreamSource();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link MultipartFile}类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default MultipartFile traceForMultipartFile(String url,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getMultipartFile();
    }

    /**
     * 发起一个[TRACE]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param tClass 实体的Class
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T traceForEntity(String url,Class<T> tClass,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getEntity(tClass);
    }

    /**
     * 发起一个[TRACE]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param typeToken 实体TypeToken
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T traceForEntity(String url,SerializationTypeToken<T> typeToken,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getEntity(typeToken);
    }

    /**
     * 发起一个[TRACE]请求，返回{@link T }类型的返回值
     * @param url URL地址，支持Rest参数占位符
     * @param type 实体Type
     * @param urlParams Rest参数占位符的填充值
     * @throws IOException
     */
    default <T> T traceForEntity(String url,Type type,Object...urlParams) throws IOException {
        Request request = Request.trace(url, urlParams);
        return execute(request).getEntity(type);
    }


    /**
     * 判断是否为文件类型的请求
     * @param params 参数列表
     * @return
     */
    default boolean isFileRequest(Map<String,Object> params){
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Class<?> requestParamType = entry.getValue().getClass();
            if(File.class== requestParamType            ||
               File[].class == requestParamType         ||
               MultipartFile.class == requestParamType  ||
               MultipartFile[].class == requestParamType)
            {
                return true;
            }
        }
        return false;
    }

}
