package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import com.luckyframework.spel.SpELRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/23 23:46
 */
public class SpELRequestProcessor implements RequestProcessor {

    private static final Pattern PATTERN = Pattern.compile("#\\{(?!\\{)(?!})[\\S\\s]+?}");

    private final SpELRuntime spELRuntime;

    public SpELRequestProcessor() {
        spELRuntime = new SpELRuntime();
    }

    @Override
    public void process(Request request) {
        DefaultRequest defaultRequest = (DefaultRequest) request;
        String urlTemplate = defaultRequest.getUrlTemplate();
        TempPair<String[], List<String>> cutPair = StringUtils.regularCut(urlTemplate, PATTERN);
        List<String> expressionList = cutPair.getTwo();
        List<Object> args = new ArrayList<>(expressionList.size());
        for (String exp : expressionList) {
            exp = exp.substring(2, exp.length() -1).trim();
            args.add(spELRuntime.getValueForType(exp));
        }
        String newUrlTemplate = StringUtils.misalignedSplice(cutPair.getOne(), args.toArray(new Object[0]));
        defaultRequest.setUrlTemplate(newUrlTemplate);
    }
}
