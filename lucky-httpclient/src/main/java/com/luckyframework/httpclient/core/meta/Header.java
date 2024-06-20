package com.luckyframework.httpclient.core.meta;

import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 请求头
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/16 9:38 上午
 */
public class Header {

    private final HeaderType headerType;
    private final String name;
    private final Object value;
    private final Map<String, String> nameValuePairMap;

    public Header(Header header) {
        this.headerType = header.headerType;
        this.name = header.name;
        this.value = header.value;
        this.nameValuePairMap = Collections.unmodifiableMap(header.initNameValuePairMap());
    }

    public Header(String name, Object value, HeaderType headerType) {
        this.name = name;
        this.headerType = headerType;
        this.value = value;
        this.nameValuePairMap = Collections.unmodifiableMap(initNameValuePairMap());
    }

    public Map<String, String> initNameValuePairMap() {
        Map<String, String> nameValuePairMap = new LinkedHashMap<>();
        if (value != null && StringUtils.hasText(value.toString().trim())) {
            String[] nameValueStrArray = value.toString().trim().split(";");
            for (String nameValueStr : nameValueStrArray) {
                int index = nameValueStr.indexOf("=");
                if (index == -1 || nameValueStr.endsWith("==")) {
                    nameValuePairMap.put(name, nameValueStr.trim());
                } else {
                    nameValuePairMap.put(nameValueStr.substring(0, index).trim(), nameValueStr.substring(index + 1));
                }
            }
        }
        return nameValuePairMap;
    }



    public static Header builderAdd(String name, Object value) {
        return new Header(name, value, HeaderType.ADD);
    }

    public static Header builderSet(String name, Object value) {
        return new Header(name, value, HeaderType.SET);
    }

    public static Header builderShow(String name, Object value) {
        return new Header(name, value, HeaderType.SHOW);
    }

    public String getInternalValue(String internalName) {
        return this.nameValuePairMap.get(internalName.toLowerCase());
    }


    public HeaderType getHeaderType() {
        return headerType;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public Map<String, String> getNameValuePairMap() {
        return nameValuePairMap;
    }

    public String getHeaderString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : nameValuePairMap.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }
        return sb.substring(0, sb.length() - 1);
    }

    @Override
    public String toString() {
        switch (headerType) {
            case ADD:
                return "(A)" + name + ": " + value;
            case SET:
                return "(S)" + name + ": " + value;
            default:
                return name + ": " + value;
        }
    }

    //-------------------------------------
    //            Map Methods
    //-------------------------------------

    public int size() {
        return this.nameValuePairMap.size();
    }

    public boolean isEmpty() {
        return this.nameValuePairMap.isEmpty();
    }

    public boolean containsKey(String name) {
        return this.nameValuePairMap.containsKey(name.toLowerCase());
    }

    public boolean containsValue(Object headerValue) {
        return this.nameValuePairMap.containsValue(headerValue);
    }


    public enum HeaderType {
        ADD, SET, SHOW
    }

}
