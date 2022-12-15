package com.luckyframework.httpclient.core;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.TempPair;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 请求头管理器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/30 10:22 上午
 */
public interface HttpHeaderManager {


    /**
     * 添加一个请求头
     * @param name 名称
     * @param header 值
     */
    void addHeader(String name,Object header);

    /**
     * 设置一个请求头
     * @param name 名称
     * @param header 值
     */
    void setHeader(String name,Object header);

    void  putHeader(String name,Object header);

    /**
     * 移除一个请求头
     * @param name
     */
    void removerHeader(String name);

    /**
     * 获取第一个匹配的头信息
     * @param name 名称
     * @return
     */
    default Header getFirstHeader(String name){
        List<Header> headerList = getHeader(name);
        if(ContainerUtils.isEmptyCollection(headerList)){
            return null;
        }
        return headerList.get(0);
    }

    /**
     * 获取最后一个匹配的头信息
     * @param name 名称
     * @return
     */
    default Header getLastHeader(String name){
        List<Header> headerList = getHeader(name);
        if(ContainerUtils.isEmptyCollection(headerList)){
            return null;
        }
        return headerList.get(headerList.size()-1);
    }

    @SuppressWarnings("unchecked")
    default ContentType getContentType(){
        Header header = getFirstHeader(HttpHeaders.CONTENT_TYPE);
        if(header == null || header.getValue() == null){
            return ContentType.DEFAULT_TEXT;
        }
        String[] contents = header.getValue().toString().split(";");
        String mimeType = null;
        List<TempPair<String,String>> paramsList = new ArrayList<>();
        for (String content : contents) {
            content = content.trim();
            if(!content.contains("=") && mimeType==null){
                mimeType = content;
            }else if(content.contains("=")){
                String[] nameValue = content.split("=");
                String name = nameValue[0].trim().toLowerCase();
                String value = nameValue.length == 1 ? "" : nameValue[1].trim().toLowerCase();
                paramsList.add(TempPair.of(name,value));
            }
        }
        mimeType = mimeType==null?ContentType.DEFAULT_TEXT.getMimeType():mimeType;
        return ContentType.create(mimeType,paramsList.toArray(new TempPair[0]));
    }

    default void setContentType(String contentType){
        setHeader(HttpHeaders.CONTENT_TYPE,contentType);
    }

    default void setContentType(ContentType contentType){
        setHeader(HttpHeaders.CONTENT_TYPE,contentType.toString());
    }

    /**
     * 获取匹配的头信息
     * @param name 名称
     * @return
     */
    List<Header> getHeader(String name);

    /**
     * 移除第一个匹配的头信息
     * @param name 名称
     */
    void removerFirstHeader(String name);

    /**
     * 移除最后一个匹配的头信息
     * @param name 名称
     */
    void removerLastHeader(String name);

    /**
     * 移除指定索引处的头信息
     * @param name 名称
     */
    void removerHeader(String name,int index);


    /**
     * 获取所有请求头名称和请求头信息的Map
     * @return
     */
    Map<String,List<Header>> getHeaderMap();

    /**
     * 获取所有的请求头信息
     * @return
     */
    default List<Header> getHeaders(){
        List<Header> headers = new LinkedList<>();
        for (List<Header> headerList : getHeaderMap().values()) {
            headers.addAll(headerList);
        }
        return headers;
    }

    /**
     * 添加基于Basic Auth方式的权限认证
     * @param username 用户名
     * @param password 密码
     */
    default void setAuthorization(String username,String password){
        String auth = username + ":" +password;
        byte[] encodeAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic "+new String(encodeAuth, StandardCharsets.UTF_8);
        addHeader(HttpHeaders.AUTHORIZATION,authHeader);
    }


    default void check(String name,Object header){
        Assert.notNull(name,"Header name is null");
        Assert.notNull(header,"Header value is null");
    }
}
