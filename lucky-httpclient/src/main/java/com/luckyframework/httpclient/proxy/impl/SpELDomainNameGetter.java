package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.DomainNameGetter;
import com.luckyframework.spel.SpELRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 支持SpEL表达式的域名获取器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 10:14
 */
public class SpELDomainNameGetter implements DomainNameGetter {

    private static final Pattern PATTERN = Pattern.compile("#\\{(?!\\{)(?!})[\\S\\s]+?}");

    private static final SpELRuntime spELRuntime = new SpELRuntime();

    @Override
    public String getDomainName(String configDomainName) {
        TempPair<String[], List<String>> cutPair = StringUtils.regularCut(configDomainName, PATTERN);
        List<String> expressionList = cutPair.getTwo();
        List<Object> args = new ArrayList<>(expressionList.size());
        for (String exp : expressionList) {
            exp = exp.substring(2, exp.length() -1).trim();
            args.add(spELRuntime.getValueForType(exp));
        }
        return StringUtils.misalignedSplice(cutPair.getOne(), args.toArray(new Object[0]));
    }
}
