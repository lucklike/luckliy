package com.luckyframework.httpclient.core.util;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HeaderMataData;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.util.MimeType;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luckyframework.httpclient.proxy.function.SerializationFunctions._url;

/**
 * 资源名称解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/5 14:13
 */
public class ResourceNameParser {

    // 匹配 filename*=charset'language'encoded-value
    // 例如: UTF-8''%E6%8A%A5%E5%91%8A.pdf
    private static final Pattern FILENAME_STAR_PATTERN =
            Pattern.compile("^([^']+)'([^']*)'(.+)$");

    /**
     * 解析 filename* 的值
     *
     * @param filenameStar 例如: "UTF-8''%E6%8A%A5%E5%91%8A.pdf"
     * @return 解码后的文件名，例如: "报告.pdf"
     */
    public static String parseFileNameX(String filenameStar) {
        Matcher matcher = FILENAME_STAR_PATTERN.matcher(filenameStar);

        if (!matcher.matches()) {
            return sanitizeFileName(filenameStar);
        }

        String charset = matcher.group(1);     // 例如: "UTF-8"
        String language = matcher.group(2);    // 例如: "" (通常为空)
        String encodedValue = matcher.group(3); // 例如: "%E6%8A%A5%E5%91%8A.pdf"

        try {
            // URL 解码
            String decoded = _url(encodedValue, charset);
            decoded = StringUtils.trimBothEndsChars(decoded.trim(), "\"").trim();

            // 清理文件名中的非法字符
            return sanitizeFileName(decoded);

        } catch (Exception e) {
            encodedValue = StringUtils.trimBothEndsChars(encodedValue.trim(), "\"").trim();
            return sanitizeFileName(encodedValue);
        }
    }

    /**
     * 解析 filename 的值
     *
     * @param filenameStar 例如: "%E6%8A%A5%E5%91%8A.pdf"
     * @return 报告.pdf
     */
    private static String parseFileName(String filenameStar) {
        try {
            filenameStar = StringUtils.trimBothEndsChars(filenameStar.trim(), "\"").trim();
            String fileName = _url(filenameStar);
            return sanitizeFileName(fileName);
        } catch (Exception e) {
            return sanitizeFileName(filenameStar);
        }
    }


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
        Header contentDispositionHeader = headerManager.getFirstHeader(HttpHeaders.CONTENT_DISPOSITION);
        // 尝试从Content-Disposition属性中获取文件名
        if (contentDispositionHeader != null) {
            // filename*
            if (contentDispositionHeader.containsKey("filename*")) {
                return parseFileNameX(contentDispositionHeader.getInternalValue("filename*"));
            }

            // filename
            if (contentDispositionHeader.containsKey("filename")) {
                return parseFileName(contentDispositionHeader.getInternalValue("filename"));
            }
        }

        // 尝试从Content-Type属性中获取文件名
        else if (headerManager.getFirstHeader(HttpHeaders.CONTENT_TYPE) != null) {
            // 尝试解析Content-Type获取文件扩展名
            MimeType headerMimeType = headerManager.getContentType().getMimeType();
            String urlResourceName = StringUtils.getUrlResourceName(headerMataData.getRequest().getUrl());
            String urlMimeType = ContentTypeUtils.getMimeType(urlResourceName);

            // Content-Type兼容URL时直接返回资源名
            if (urlMimeType != null && headerMimeType.isCompatibleWith(MimeType.valueOf(urlMimeType))) {
                return sanitizeFileName(urlResourceName);
            }

            // 从Content-Type中解析不出来文件类型直接返回资源名
            String fileExtension = ContentTypeUtils.getFileExtension(headerMimeType.toString());
            if (!StringUtils.hasText(fileExtension)) {
                return sanitizeFileName(urlResourceName);
            }

            // 可以解析时
            return sanitizeFileName(StringUtils.getFilename(urlResourceName) + "." + fileExtension);
        }

        return sanitizeFileName(StringUtils.getUrlResourceName(headerMataData.getRequest().getUrl()));
    }

    /**
     * 清理文件名中的非法字符
     * 移除路径分隔符，防止路径遍历攻击
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        // 移除或替换非法字符
        return fileName
                .replaceAll("[/\\\\:*?\"<>|]", "_")  // Windows/Unix 非法字符替换为下划线
                .replaceAll("\\.\\.", "_")           // 防止路径遍历
                .trim();
    }

}
