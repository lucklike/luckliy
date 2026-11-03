package com.luckyframework.httpclient.core.util;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HeaderMataData;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.util.MimeType;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 资源名称解析器
 * <p>
 * 用于在文件下载场景推断需要保存的文件名，解析优先级为：
 * Content-Disposition（filename* 扩展形式 &gt; filename 常规形式，并支持 RFC 2231/6266 的分段延续）
 * &gt; Content-Type（结合请求 URL 推导扩展名）&gt; 请求 URL 末段资源名。
 * <p>
 * 注意：响应头参数已由 {@link HttpHeaderParser#parseHeaderSimple(String)} 统一做过一次 URL 解码，
 * 因此本类不再对参数值进行二次解码，避免把文件名中合法的 '+'、'%' 等字符误解码损坏。
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/5 14:13
 */
public class ResourceNameParser {

    /**
     * 匹配 filename* 的扩展值形式：charset'language'percent-encoded-value
     * <p>
     * 由于头部在进入本类前已统一解码一次，这里第三段取到的是已解码文本（例如：UTF-8''报告.pdf）
     */
    private static final Pattern FILENAME_STAR_PATTERN =
            Pattern.compile("^([^']*)'([^']*)'(.*)$");

    /**
     * 匹配 RFC 2231/6266 的分段延续参数名：filename*0、filename*0*、filename*1* ...
     */
    private static final Pattern FILENAME_SEGMENT_PATTERN =
            Pattern.compile("^filename\\*(\\d+)\\*?$", Pattern.CASE_INSENSITIVE);

    /**
     * Windows 平台保留设备名（不区分大小写），命中时需要加前缀避免创建失败
     */
    private static final Pattern WINDOWS_RESERVED_NAME_PATTERN =
            Pattern.compile("^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])(\\..*)?$", Pattern.CASE_INSENSITIVE);

    /**
     * 尝试获取当前正在下载的文件名，这种获取方式要求响应头中必须提供
     * Content-Disposition属性或者Content-Type属性。如果提供了Content-Disposition属性
     * 则会优先从该属性中的filename*（其次filename）选项中获取文件名，否则则会从Content-Type中获取文件类型后生成一个
     * 随机的文件名,如果从Content-Type中也无法解析出文件类型，则会尝试从请求的URL中获取文件名
     *
     * @param headerMataData 响应头元信息
     * @return 当前正在下载的文件名
     */
    public static String getResourceName(HeaderMataData headerMataData) {
        HttpHeaderManager headerManager = headerMataData.getHeaderManager();
        String urlResourceName = getUrlResourceName(headerMataData);

        // 1.尝试从Content-Disposition属性中获取文件名
        Header contentDispositionHeader = headerManager.getFirstHeader(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDispositionHeader != null) {
            Map<String, String> parameters = contentDispositionHeader.getNameValuePairMap();
            String dispositionFileName = resolveFileName(parameters);
            if (StringUtils.hasText(dispositionFileName)) {
                return sanitizeFileName(dispositionFileName);
            }
        }

        // 2.尝试从Content-Type属性中推导文件名
        if (headerManager.getFirstHeader(HttpHeaders.CONTENT_TYPE) != null) {
            MimeType headerMimeType = headerManager.getContentType().getMimeType();
            String urlMimeType = StringUtils.hasText(urlResourceName)
                    ? ContentTypeUtils.getMimeType(urlResourceName) : null;

            // Content-Type与URL推导出的类型兼容时直接使用URL资源名
            MimeType urlMimeTypeObject = safeValueOfMimeType(urlMimeType);
            if (urlMimeTypeObject != null && headerMimeType.isCompatibleWith(urlMimeTypeObject)) {
                return sanitizeFileName(urlResourceName);
            }

            // 无法从Content-Type解析出扩展名时直接返回URL资源名
            String fileExtension = ContentTypeUtils.getFileExtension(baseMimeType(headerMimeType));
            if (!StringUtils.hasText(fileExtension)) {
                return sanitizeFileName(urlResourceName);
            }

            // 可以解析出扩展名时，用URL名（去扩展名）拼接上真实扩展名
            return sanitizeFileName(StringUtils.getFilename(urlResourceName) + "." + fileExtension);
        }

        // 3.兜底：使用请求URL中的资源名
        return sanitizeFileName(urlResourceName);
    }

    /**
     * 按 RFC 6266 优先级从 Content-Disposition 参数中解析文件名：
     * 扩展形式 filename*（含分段延续 filename*N / filename*N*）优先于常规形式 filename。
     *
     * @param parameters 已解码的头部参数Map（Key不区分大小写）
     * @return 解析出的文件名，解析不到时返回null
     */
    private static String resolveFileName(Map<String, String> parameters) {
        // 完整扩展形式
        String extended = parameters.get("filename*");
        if (StringUtils.hasText(extended)) {
            return stripCharsetPrefix(extended);
        }

        // 分段延续形式：filename*0、filename*0*、filename*1 ...
        String segmented = resolveSegmentedFileName(parameters);
        if (StringUtils.hasText(segmented)) {
            return segmented;
        }

        // 常规形式
        return parameters.get("filename");
    }

    /**
     * 按序号顺序拼接分段延续的文件名参数，仅首段需要剥离 charset'language' 前缀
     *
     * @param parameters 已解码的头部参数Map
     * @return 拼接后的文件名，无分段参数时返回null
     */
    private static String resolveSegmentedFileName(Map<String, String> parameters) {
        TreeMap<Integer, String> orderedSegments = new TreeMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            Matcher matcher = FILENAME_SEGMENT_PATTERN.matcher(entry.getKey());
            if (matcher.matches()) {
                try {
                    orderedSegments.put(Integer.parseInt(matcher.group(1)), entry.getValue());
                } catch (NumberFormatException ignore) {
                    // 序号非法时跳过该分段
                }
            }
        }
        if (orderedSegments.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        boolean firstSegment = true;
        for (String segment : orderedSegments.values()) {
            if (segment == null) {
                continue;
            }
            builder.append(firstSegment ? stripCharsetPrefix(segment) : segment);
            firstSegment = false;
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * 剥离扩展值中的 charset'language' 前缀，仅保留文件名部分；不符合前缀格式时原样返回
     *
     * @param extendedValue 例如: "UTF-8''报告.pdf"
     * @return 去掉前缀后的文件名，例如: "报告.pdf"
     */
    private static String stripCharsetPrefix(String extendedValue) {
        Matcher matcher = FILENAME_STAR_PATTERN.matcher(extendedValue);
        if (matcher.matches()) {
            return matcher.group(3);
        }
        return extendedValue;
    }

    /**
     * 安全地获取请求URL对应的资源名，request/url为空时返回空串（避免下游NPE）
     *
     * @param headerMataData 响应头元信息
     * @return URL资源名，取不到时为空串
     */
    private static String getUrlResourceName(HeaderMataData headerMataData) {
        Request request = headerMataData.getRequest();
        if (request == null || !StringUtils.hasText(request.getUrl())) {
            return "";
        }
        String resourceName = StringUtils.getUrlResourceName(request.getUrl());
        return resourceName == null ? "" : resourceName;
    }

    /**
     * 安全地将MIME字符串转为MimeType对象，字符串为空或非法时返回null
     *
     * @param mimeType MIME字符串
     * @return MimeType对象或null
     */
    private static MimeType safeValueOfMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return null;
        }
        try {
            return MimeType.valueOf(mimeType);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 取MimeType的"type/subtype"基本形式（不含charset等参数），替代手工截取分号
     *
     * @param mimeType MimeType对象
     * @return 不含参数的MIME字符串
     */
    private static String baseMimeType(MimeType mimeType) {
        return mimeType.getType() + "/" + mimeType.getSubtype();
    }

    /**
     * 清理文件名中的非法字符
     * <pre>
     *     1.替换Windows/Unix非法字符及控制字符为下划线
     *     2.将'..'替换为下划线，防止路径遍历
     *     3.去除结尾的点和空格（Windows不允许）
     *     4.命中Windows保留设备名时加下划线前缀
     * </pre>
     *
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String sanitized = fileName
                .replaceAll("[\\x00-\\x1F\\x7F]", "_")   // 控制字符
                .replaceAll("[/\\\\:*?\"<>|]", "_")       // Windows/Unix 非法字符
                .replaceAll("\\.\\.", "_");               // 防止路径遍历

        // 去除结尾的点和空格
        int end = sanitized.length();
        while (end > 0 && (sanitized.charAt(end - 1) == '.' || sanitized.charAt(end - 1) == ' ')) {
            end--;
        }
        sanitized = sanitized.substring(0, end).trim();

        // Windows保留设备名保护
        if (WINDOWS_RESERVED_NAME_PATTERN.matcher(sanitized).matches()) {
            sanitized = "_" + sanitized;
        }
        return sanitized;
    }
}
