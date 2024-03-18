package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.SpELUtils;

import java.util.List;
import java.util.Map;

public class DefaultSpElInfoCache implements SpElInfoCache{
    @Override
    public void initialize(Context context, SpELUtils.ExtraSpELArgs spELArgs) {

    }

    @Override
    public List<String> getImportPackages() {
        return null;
    }

    @Override
    public Map<String, Object> getAnnRootArgMap() {
        return null;
    }

    @Override
    public Map<String, Object> getAnnVariableMap() {
        return null;
    }
}
