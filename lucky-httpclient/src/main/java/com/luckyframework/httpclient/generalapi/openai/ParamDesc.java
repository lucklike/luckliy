package com.luckyframework.httpclient.generalapi.openai;

import com.luckyframework.common.ConfigurationMap;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * 简单参数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/25 14:50
 */
public class ParamDesc {

    private static final String TYPE_STRING = "string";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_INTEGER = "integer";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_NULL = "null";


    private final String paramName;
    private final String paramType;
    private final ConfigurationMap config = new ConfigurationMap();

    private ParamDesc(@NonNull String paramName, @NonNull String paramType) {
        Assert.notNull(paramName, "paramName must not be null");
        Assert.notNull(paramType, "paramType must not be null");
        this.paramName = paramName;
        this.paramType = paramType;
    }

    public static ParamDesc of(@NonNull String paramName, @NonNull String paramType) {
        return new ParamDesc(paramName, paramType);
    }

    public static ParamDesc string(@NonNull String paramName) {
        return of(paramName, TYPE_STRING);
    }

    public static ParamDesc number(@NonNull String paramName) {
        return of(paramName, TYPE_NUMBER);
    }

    public static ParamDesc integer(@NonNull String paramName) {
        return of(paramName, TYPE_INTEGER);
    }

    public static ParamDesc bool(@NonNull String paramName) {
        return of(paramName, TYPE_BOOLEAN);
    }

    public static ParamDesc object(@NonNull String paramName) {
        return of(paramName, TYPE_OBJECT);
    }

    public static ParamDesc array(@NonNull String paramName) {
        return of(paramName, TYPE_ARRAY);
    }

}
