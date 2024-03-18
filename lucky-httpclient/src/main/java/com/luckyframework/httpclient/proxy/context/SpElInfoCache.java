package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.SpELUtils;

import java.util.List;
import java.util.Map;

public interface SpElInfoCache {

    void initialize(Context context, SpELUtils.ExtraSpELArgs spELArgs);

    List<String> getImportPackages();

    Map<String, Object> getAnnRootArgMap();

    Map<String, Object> getAnnVariableMap();

}
