package com.luckyframework.httpclient.core;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.io.HttpResource;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 *         该类型参数使用({@link Map<String, Object>})进行封装，用来添加一些表单参数，支持的数据类型有如下几种：<br/>
 *         1.{@link java.io.File File}表示的单个文件<br/>
 *         2.{@link java.io.File[] File[]}表示的多个文件<br/>
 *         3.{@link MultipartFile MultipartFile}表示的单个文件<br/>
 *         4.{@link MultipartFile[] MultipartFile[]}表示的多个文件<br/>
 *         5.{@link HttpFile HttpFile}表示的单个文件<br/>
 *         6.{@link HttpFile[] HttpFile[]}表示的多个文件<br/>
 *         5.{@link Resource Resource}表示的单个文件<br/>
 *         6.{@link Resource[] Resource[]}表示的多个文件<br/>
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

    /**
     * 设置请求体参数
     */
    RequestParameter setBody(BodyObject body);

    /**
     * 返回请求体参数
     */
    BodyObject getBody();

    /**
     * 添加一个Rest参数
     */
    RequestParameter addPathParameter(String name, Object value);

    /**
     * 设置请求参数
     */
    RequestParameter setPathParameter(Map<String, Object> pathParamMap);

    /**
     * 添加一个请求参数
     */
    RequestParameter addRequestParameter(String name, Object value);

    /**
     * 设置请求参数
     */
    RequestParameter setRequestParameter(Map<String, Object> requestParamMap);

    /**
     * 添加一个URL参数
     */
    RequestParameter addQueryParameter(String name, Object value);

    /**
     * 设置一个URL参数
     */
    RequestParameter setQueryParameter(String name, Object value);

    RequestParameter setQueryParameters(Map<String, List<Object>> queryParameters);

    /**
     * 移除一个请求参数
     */
    RequestParameter removerRequestParameter(String name);

    /**
     * 移除一个Rest参数
     */
    RequestParameter removerPathParameter(String name);

    /**
     * 移除一个URL参数
     */
    RequestParameter removerQueryParameter(String name);

    /**
     * 移除指定位置处的URL参数
     */
    RequestParameter removerQueryParameter(String name, int index);


    //-------------------------------------------------------------------
    //              Default Add Parameter Methods
    //-------------------------------------------------------------------

    /**
     * 添加一个表单参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    default RequestParameter addFormParameter(String name, Object value) {
        addRequestParameter(name, value);
        return this;
    }

    /**
     * 添加一个组Http文件参数
     *
     * @param name      参数名
     * @param httpFiles Http文件列表
     */
    default RequestParameter addHttpFiles(String name, HttpFile... httpFiles) {
        addRequestParameter(name, httpFiles);
        return this;
    }

    /**
     * 添加一个流式参数
     *
     * @param name        参数名
     * @param fileName    文件名
     * @param inputStream 输入流
     */
    default RequestParameter addInputStream(String name, String fileName, InputStream inputStream) {
        addHttpFiles(name, new HttpFile(inputStream, name));
        return this;
    }

    /**
     * 添加一组文件参数
     *
     * @param name  参数名
     * @param files 文件列表
     */
    default RequestParameter addFiles(String name, File... files) {
        addRequestParameter(name, files);
        return this;
    }

    /**
     * 添加一组文件参数
     *
     * @param name      参数名
     * @param filePaths 文件路径列表
     */
    default RequestParameter addFiles(String name, String... filePaths) {
        addFiles(name, ConversionUtils.conversion(filePaths, File[].class));
        return this;
    }

    /**
     * 添加一组资源参数
     *
     * @param name      参数名
     * @param resources 资源列表
     */
    default RequestParameter addResources(String name, Resource... resources) {
        addRequestParameter(
                name,
                // 如果是UrlResource则需要转化成HttpResource
                Stream.of(resources)
                        .map(r -> {
                            if (r instanceof UrlResource) {
                                return new HttpResource(((UrlResource) r));
                            }
                            return r;
                        })
                        .toArray(Resource[]::new)
        );
        return this;
    }

    /**
     * 添加一组资源参数
     *
     * @param name          参数名
     * @param resourcePaths 资源路径列表
     */
    default RequestParameter addResources(String name, String... resourcePaths) {
        addResources(name, ConversionUtils.conversion(resourcePaths, Resource[].class));
        return this;
    }

    /**
     * 添加一组MultipartFile参数
     *
     * @param name           参数名
     * @param multipartFiles MultipartFile参数列表
     */
    default RequestParameter addMultipartFiles(String name, MultipartFile... multipartFiles) {
        addRequestParameter(name, multipartFiles);
        return this;
    }

    /**
     * 设置一个JSON类型的Body参数
     *
     * @param jsonBody 可序列化为JSON字符的对象
     */
    default RequestParameter setJsonBody(Object jsonBody) {
        setBody(BodyObject.jsonBody(jsonBody));
        return this;
    }

    /**
     * 设置一个JSON类型的Body参数
     *
     * @param jsonBodyString JSON字符串
     */
    default RequestParameter setJsonBody(String jsonBodyString) {
        setBody(BodyObject.jsonBody(jsonBodyString));
        return this;
    }

    /**
     * 设置一个XML类型的Body参数
     *
     * @param xmlBody 可序列化为XML字符的对象
     */
    default RequestParameter setXmlBody(Object xmlBody) {
        setBody(BodyObject.jsonBody(xmlBody));
        return this;
    }

    /**
     * 设置一个JSON类型的Body参数
     *
     * @param xmlBodyString XML字符串
     */
    default RequestParameter setXmlBody(String xmlBodyString) {
        setBody(BodyObject.jsonBody(xmlBodyString));
        return this;
    }


}
