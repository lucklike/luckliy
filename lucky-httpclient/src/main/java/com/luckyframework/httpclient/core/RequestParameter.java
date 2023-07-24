package com.luckyframework.httpclient.core;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 请求参数
 * <ul>
 *     <li>
 *         Url Parameter: <br/>
 *         该类型参数使用({@link Map<String,Object>})进行封装，最终会以字符串的形式拼接到URL中.<br/>
 *         eg: <br/>
 *         URL : http://localhost:8864/lucky<br/>
 *         Url Parameter: [name=Jack,id=123]<br/>
 *         ==><br/>
 *         http://localhost:8864/lucky?name=Jack&id=123
 *     </li>
 *     <li>
 *         Rest Parameter:<br/>
 *         该类型参数使用({@link Map<String,Object>})进行封装，主要用来填充URL中的Rest占位符.<br/>
 *         eg: <br/>
 *         URL : http://localhost:8864/lucky/{name}?id={id}<br/>
 *         Url Parameter: [name=Jack,id=123]<br/>
 *         ==><br/>
 *         http://localhost:8864/lucky/Jack?id=123
 *     </li>
 *     <li>
 *         Request Parameter:<br/>
 *         该类型参数使用({@link Map<String,Object>})进行封装，用来添加一些表单参数，支持的数据类型有如下几种：<br/>
 *         1.{@link java.io.File File}表示的单个文件<br/>
 *         2.{@link java.io.File File[]}表示的多个文件<br/>
 *         3.{@link MultipartFile MultipartFile}表示的单个文件<br/>
 *         4.{@link MultipartFile MultipartFile[]}表示的多个文件<br/>
 *         5.其他类型的Value都将会被当作字符串来处理
 *     </li>
 *     <li>
 *         Body Parameter:<br/>
 *         该类型参数使用({@link BodyObject})进行封装,该参数最终会被放入请求体中。
 *     </li>
 * </ul>
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:44 下午
 */
public interface RequestParameter {

    /**
     * 请求的参数
     */
    Map<String, Object> getRequestParameters();

    /**
     * rest风格参数，在URL中以占位符形式的存在的参数
     */
    Map<String, Object> getPathParameters();

    /**
     * URL参数，这些参数将以字符串的形式拼接到URL中
     */
    Map<String, List<Object>> getQueryParameters();

    void setBody(BodyObject body);

    BodyObject getBody();

    /**
     * 添加一个Rest参数
     */
    void addPathParameter(String name, Object value);

    /**
     * 设置请求参数
     */
    void setPathParameter(Map<String, Object> pathParamMap);

    /**
     * 添加一个请求参数
     */
    void addRequestParameter(String name, Object value);

    /**
     * 设置请求参数
     */
    void setRequestParameter(Map<String, Object> requestParamMap);

    /**
     * 添加一个URL参数
     */
    void addQueryParameter(String name, Object value);

    /**
     * 设置一个URL参数
     */
    void setQueryParameter(String name, Object value);

    void setQueryParameters(Map<String, List<Object>> queryParameters);

    /**
     * 移除一个请求参数
     */
    void removerRequestParameter(String name);

    /**
     * 移除一个Rest参数
     */
    void removerPathParameter(String name);

    /**
     * 移除一个URL参数
     */
    void removerQueryParameter(String name);

    /**
     * 移除指定位置处的URL参数
     */
    void removerQueryParameter(String name, int index);


    //-------------------------------------------------------------------
    //              Default Add Parameter Methods
    //-------------------------------------------------------------------

    /**
     * 添加一个流式参数
     *
     * @param name        参数名
     * @param fileName    文件名
     * @param inputStream 输入流
     */
    default void addInputStream(String name, String fileName, InputStream inputStream) {
        MultipartFile mf = new MultipartFile(inputStream, fileName);
        addRequestParameter(name, mf);
    }

    default void addFiles(String name, File... files) {
        addRequestParameter(name, files);
    }

    default void addFiles(String name, String... filePaths) {
        addFiles(name, ConversionUtils.conversion(filePaths, File[].class));
    }

    default void addResources(String name, Resource... resources) {
        addRequestParameter(name, resources);
    }

    default void addResources(String name, String... resourcePaths){
        addResources(name, ConversionUtils.conversion(resourcePaths, Resource[].class));
    }

    default void addMultipartFiles(String name, MultipartFile... multipartFiles){
        addRequestParameter(name, multipartFiles);
    }

    /**
     * 设置一个JSON类型的Body参数
     *
     * @param jsonBody 可序列化为JSON字符的对象
     */
    default void setJsonBody(Object jsonBody) {
        setBody(BodyObject.jsonBody(jsonBody));
    }

    /**
     * 设置一个JSON类型的Body参数
     *
     * @param jsonBodyString JSON字符串
     */
    default void setJsonBody(String jsonBodyString) {
        setBody(BodyObject.jsonBody(jsonBodyString));
    }

    /**
     * 设置一个XML类型的Body参数
     *
     * @param xmlBody 可序列化为XML字符的对象
     */
    default void setXmlBody(Object xmlBody) {
        setBody(BodyObject.jsonBody(xmlBody));
    }

    /**
     * 设置一个JSON类型的Body参数
     *
     * @param xmlBodyString XML字符串
     */
    default void setXmlBody(String xmlBodyString) {
        setBody(BodyObject.jsonBody(xmlBodyString));
    }


}
