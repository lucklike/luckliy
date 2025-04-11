package com.luckyframework.httpclient.generalapi.openai;

import com.luckyframework.common.ConfigurationMap;

/**
 * 参数描述
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/25 14:50
 */
public class ParamDesc extends ConfigurationMap {

    public static final String KEY_TYPE = "type";
    public static final String KEY_DESC = "description";

    public void setType(String type) {
        addProperty(KEY_TYPE, type);
    }

    public String getType() {
        return getString(KEY_TYPE);
    }

    public void setDescription(String description) {
        addProperty(KEY_DESC, description);
    }

    public String getDescription() {
        return getString(KEY_DESC);
    }

}
