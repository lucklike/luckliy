package com.luckyframework.httpclient.core;

import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TimeUtils;
import com.luckyframework.web.ContentTypeUtils;

/**
 * 资源名称解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/5 14:13
 */
public class ResourceNameParser {

    /**
     * 尝试获取当前正在下载的文件名，这种获取方式要求响应头中必须提供
     * Content-Disposition属性或者Content-Type属性。如果提供了Content-Disposition属性
     * 则会优先从该属性中的filename选项中获取文件名，否则则会从Content-Type中获取文件类型后生成一个
     * 随机的文件名,如果从Content-Type中也无法解析出文件类型，则会尝试从请求的URL中获取文件名
     *
     * @param headerMataData 响应头元信息
     * @return 当前正在下载的文件名
     */
    public static String getResourceName(HeaderMataData headerMataData) {
        HttpHeaderManager headerManager = headerMataData.getHeaderManager();
        Header header = headerManager.getFirstHeader(HttpHeaders.CONTENT_DISPOSITION);
        // 尝试从Content-Disposition属性中获取文件名
        if (header != null && header.containsKey("filename")) {
            return StringUtils.trimBothEndsChars(header.getInternalValue("filename").trim(), "\"").trim();
        }
        // 尝试从Content-Type属性中获取文件名
        else if (headerManager.getFirstHeader(HttpHeaders.CONTENT_TYPE) != null) {
            // 尝试解析Content-Type获取文件扩展名
            String fileExtension = ContentTypeUtils.getFileExtension(headerManager.getContentType().getMimeType());
            String urlResourceName = StringUtils.getUrlResourceName(headerMataData.getRequest().getUrl());

            if (fileExtension != null) {
                // URL资源名称与Content-Type对应的扩展名能对应时，返回资源名称
                if (urlResourceName.endsWith("." + fileExtension)) {
                    return urlResourceName;
                }
                return StringUtils.format("{}-{}.{}", NanoIdUtils.randomNanoId(5), TimeUtils.formatYyyyMMdd(), fileExtension);
            }
            // 尝试从URL路径中解析文件名
            return urlResourceName;
        } else {
            return StringUtils.getUrlResourceName(headerMataData.getRequest().getUrl());
        }
    }
}
