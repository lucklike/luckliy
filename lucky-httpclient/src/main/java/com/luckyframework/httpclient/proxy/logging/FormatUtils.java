package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Header;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.serializable.JacksonSerializationScheme.prettyPrinting;
import static com.luckyframework.serializable.JaxbXmlSerializationScheme.prettyPrintByTransformer;

/**
 * 格式工具类
 */
public class FormatUtils {

    public static final String EMPTY_STRING = "";
    public static final String LINE_BREAK = "\n";
    public static final String TAB = "\t";
    public static final String FORM_DELIMITER = "&";
    public static final String HEADER_DELIMITER = ": ";
    public static final String HEADER_NAME_DELIMITER = "-";
    public static final String HEADER_VALUE_DELIMITER = "; ";



    /**
     * 拼接指定数量的制表符
     *
     * @param count 拼接数量
     * @return 拼接后的字符串
     */
    public static String appendTABCharacters(int count) {
        if (count <= 0) {
            return EMPTY_STRING;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(TAB);
        }
        return sb.toString();
    }

    /**
     * 格式化JSON字符串
     *
     * @param jsonStr   JSON字符串
     * @param moveRight 右移单位
     * @return 格式化之后的JSON字符串
     */
    public static String formatJson(String jsonStr, int... moveRight) {
        String formatReplacementSymbol = LINE_BREAK;
        if (moveRight.length > 0) {
            int count = moveRight[0];
            formatReplacementSymbol = LINE_BREAK + appendTABCharacters(count);
        }
        try {
            String json = prettyPrinting(jsonStr);
            json = json.replace(LINE_BREAK, formatReplacementSymbol);
            return formatReplacementSymbol + json;
        } catch (Exception e) {
            return formatReplacementSymbol + jsonStr;
        }
    }


    /**
     * 格式化XML字符串
     *
     * @param xmlStr    XML字符串
     * @param moveRight 右移单位
     * @return 格式化之后的XML字符串
     */
    public static String formatXml(String xmlStr, int... moveRight) {
        try {
            return prettyPrintByTransformer(xmlStr, moveRight.length > 0 ? moveRight[0] * 4 : 0, false);
        } catch (Exception e) {
            return xmlStr;
        }
    }

    /**
     * 格式化Form表单格式字符串
     *
     * @param formStr   Form表单格式字符串
     * @param moveRight 右移单位
     * @return 格式化之后的Form表单格式字符串
     */
    public static String formatForm(String formStr, int... moveRight) {
        String formatReplacementSymbol = FORM_DELIMITER;
        if (moveRight.length > 0) {
            int count = moveRight[0];
            formatReplacementSymbol = FORM_DELIMITER + LINE_BREAK + appendTABCharacters(count);
        }
        return formStr.replace(FORM_DELIMITER, formatReplacementSymbol);
    }

    public static String format(String str, int... moveRight) {
        if (moveRight.length > 0) {
            return appendTABCharacters(moveRight[0]) + str;
        }
        return str;
    }

    /**
     * 头过滤
     *
     * @param sourceHeader 头信息
     * @return 过滤之后的头信息
     */
    public static List<Header> filterHeader(List<Header> sourceHeader) {
        List<Header> resultHeaders = new ArrayList<>();
        for (Header header : sourceHeader) {
            if (header.getHeaderType() == Header.HeaderType.SET) {
                resultHeaders.clear();
            }
            resultHeaders.add(header);
        }
        return resultHeaders;
    }

    /**
     * 获取标准格式的头名称
     *
     * @param name 头名称
     * @return 标准格式的头名称
     */
    public static String getStandardHeaderName(String name) {
        List<String> strings = Stream.of(name.split(HEADER_NAME_DELIMITER)).map(StringUtils::capitalize).collect(Collectors.toList());
        return StringUtils.join(strings, HEADER_NAME_DELIMITER);
    }


}
