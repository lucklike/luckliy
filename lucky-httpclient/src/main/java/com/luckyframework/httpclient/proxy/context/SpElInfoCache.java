package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.SpELUtils;

public interface SpElInfoCache {

    void initialize(Context context, SpELUtils.ExtraSpELArgs spELArgs);

}
