package com.luckyframework.webmvc.webcore;

import com.luckyframework.common.KeyCaseSensitivityMap;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.HttpHeaders;
import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.*;

@SuppressWarnings("all")
public abstract class RequestUtils {

    //--------------------------------------------------------------------------
    //                      Request Parameters
    //--------------------------------------------------------------------------

    public static <T> T getAllParameterForObject(SerializationTypeToken<T> requiredType) {
        return (T) getAllParameterForObject(requiredType.getType());
    }

    public static <T> T getAllParameterForObject(Type requiredType) {
        return (T) getAllParameterForObject(ResolvableType.forType(requiredType));
    }

    public static <T> T getAllParameterForObject(Class<T> requiredType) {
        return (T) getAllParameterForObject(ResolvableType.forRawClass(requiredType));
    }

    public static <T> T getAllParameterForObject(ResolvableType requiredType) {
        Map<String, String[]> parameterMap = getRequest().getParameterMap();
        return (T) ConversionUtils.conversion(parameterMap, requiredType);
    }

    public static Map<String, String[]> getRequestParameterMap() {
        return getRequest().getParameterMap();
    }

    public static <T> T getRequestParameter(String parameterName, Type requiredType) {
        return ConversionUtils.conversion(getRequestParameter(parameterName), requiredType);
    }

    public static <T> T getRequestParameter(String parameterName, Class<T> requiredType) {
        return ConversionUtils.conversion(getRequestParameter(parameterName), requiredType);
    }

    public static String getRequestParameter(String parameterName) {
        return getRequest().getParameter(parameterName);
    }

    public static Object getRequestParameterValues(String parameterName, Type requiredType) {
        return ConversionUtils.conversion(getRequestParameterValues(parameterName), requiredType);
    }

    public static Object getRequestParameterValues(String parameterName, Class<?> requiredType) {
        return ConversionUtils.conversion(getRequestParameterValues(parameterName), requiredType);
    }

    public static String[] getRequestParameterValues(String parameterName) {
        return getRequest().getParameterValues(parameterName);
    }

    public static void setRequestAttribute(String name, Object attribute) {
        getRequest().setAttribute(name, attribute);
    }

    public static Object getRequestAttribute(String attributeName) {
        return getRequest().getAttribute(attributeName);
    }

    public static Object getRequestAttribute(String attributeName, ResolvableType requiredType) {
        return ConversionUtils.conversion(getRequestAttribute(attributeName), requiredType);
    }

    public static <T> T getRequestAttribute(String attributeName, Type requiredType) {
        return ConversionUtils.conversion(getRequestAttribute(attributeName), requiredType);
    }

    public static <T> T getRequestAttribute(String attributeName, SerializationTypeToken<T> requiredType) {
        return ConversionUtils.conversion(getRequestAttribute(attributeName), requiredType);
    }

    public static <T> T getRequestAttribute(String attributeName, Class<T> requiredType) {
        return ConversionUtils.conversion(getRequestAttribute(attributeName), requiredType);
    }

    //--------------------------------------------------------------------------
    //                      Header Parameters
    //--------------------------------------------------------------------------

    public static <T> T getAllHeaderForObject(SerializationTypeToken<T> requiredType) {
        return getAllHeaderForObject(requiredType.getType());
    }

    public static <T> T getAllHeaderForObject(Type requiredType) {
        return getAllHeaderForObject(requiredType);
    }

    public static <T> T getAllHeaderForObject(Class<T> requiredType) {
        return getAllHeaderForObject(ResolvableType.forRawClass(requiredType));
    }

    public static <T> T getAllHeaderForObject(ResolvableType requiredType) {
        Map<String, List<String>> headersMap = getAllHeaderMap();
        return (T) ConversionUtils.conversion(headersMap, requiredType);
    }

    public static Map<String, List<String>> getAllHeaderMap() {
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        Enumeration<String> headerNames = getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headerMap.put(headerName, getHeaders(headerName));
        }
        return new KeyCaseSensitivityMap<>(headerMap);
    }

    public static Object getHeadersForObject(String name, ResolvableType requiredType) {
        return ConversionUtils.conversion(getHeaders(name), requiredType);
    }

    public static <T> T getHeadersForObject(String name, Type requiredType) {
        return ConversionUtils.conversion(getHeaders(name), requiredType);
    }

    public static <T> T getHeadersForObject(String name, SerializationTypeToken<T> requiredType) {
        return ConversionUtils.conversion(getHeaders(name), requiredType);
    }

    public static <T> T getHeadersForObject(String name, Class<T> requiredType) {
        return ConversionUtils.conversion(getHeaders(name), requiredType);
    }

    public static List<String> getHeaders(String name) {
        List<String> headerList = new LinkedList<>();
        Enumeration<String> headers = getRequest().getHeaders(name);
        while (headers.hasMoreElements()) {
            headerList.add(headers.nextElement());
        }
        return headerList;
    }

    public static Object getHeaderForObject(String name, ResolvableType requiredType) {
        return ConversionUtils.conversion(getHeader(name), requiredType);
    }

    public static <T> T getHeaderForObject(String name, Type requiredType) {
        return ConversionUtils.conversion(getHeader(name), requiredType);
    }

    public static <T> T getHeaderForObject(String name, SerializationTypeToken<T> requiredType) {
        return ConversionUtils.conversion(getHeader(name), requiredType);
    }

    public static <T> T getHeaderForObject(String name, Class<T> requiredType) {
        return ConversionUtils.conversion(getHeader(name), requiredType);
    }

    public static String getHeader(String name) {
        return getRequest().getHeader(name);
    }

    public static Map<String, String> getHeaderMap() {
        Map<String, String> headerMap = new LinkedHashMap<>();
        HttpServletRequest request = getRequest();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headerMap.put(headerName, request.getHeader(headerName));
        }
        return new KeyCaseSensitivityMap<>(headerMap);
    }

    public List<String> getContentTypes() {
        return getHeaders(HttpHeaders.CONTENT_TYPE);
    }

    public List<String> getAccept() {
        return getHeaders(HttpHeaders.ACCEPT);
    }

    //--------------------------------------------------------------------------
    //                      Cookie Parameters
    //--------------------------------------------------------------------------

    /**
     * 根据"name"获取Cookit中的文本信息,并转化为指定的编码格式
     *
     * @param name     NAME
     * @param encoding 编码方式
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getCookieContent(String name, String encoding) throws UnsupportedEncodingException {
        String info = null;
        Cookie[] cookies = getRequest().getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                info = cookie.getValue();
                info = URLDecoder.decode(info, encoding);
            }
        }
        return info;
    }

    public static <T> T getCookieContent(String name, String encoding, Class<T> requiredType) throws UnsupportedEncodingException {
        return ConversionUtils.conversion(getCookieContent(name, encoding), requiredType);
    }

    public static String getCookieContent(String name) throws UnsupportedEncodingException {
        return getCookieContent(name, "UTF-8");
    }

    public static <T> T getCookieContent(String name, Class<T> requiredType) throws UnsupportedEncodingException {
        return ConversionUtils.conversion(getCookieContent(name), requiredType);
    }

    public static void setCookieContent(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        getResponse().addCookie(cookie);
    }


    public static RequestMethod getRequestMethod() {
        return getWebContext().getRequestMethod();
    }

    public static void setRequestMethod(RequestMethod requestMethod) {
        getWebContext().setRequestMethod(requestMethod);
    }

    public static HttpServletRequest getRequest() {
        return getWebContext().getRequest();
    }

    public static HttpServletResponse getResponse() {
        return getWebContext().getResponse();
    }

    public static HttpSession getSession() {
        return getWebContext().getSession();
    }

    public static ServletContext getServletContext() {
        return getWebContext().getApplication();
    }

    private static WebContext getWebContext() {
        return WebContext.getCurrentContext();
    }

}
