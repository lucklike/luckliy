package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SpEL转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 16:30
 */
public class SpELConvert {

    private static final Pattern PATTERN = Pattern.compile("#\\{(?!\\{)(?!})[\\S\\s]+?}");

    private final SpELRuntime spELRuntime;

    public SpELConvert(SpELRuntime spELRuntime) {
        this.spELRuntime = spELRuntime;
    }

    public SpELConvert(){
        this(new SpELRuntime());
    }

    public SpELConvert importPackage(String... packageNames) {
        ParamWrapper commonParams = spELRuntime.getCommonParams();
        for (String  packageName : packageNames) {
            commonParams.importPackage(packageName);
        }
        return this;
    }

    public SpELConvert importPackage(Class<?> aClass) {
        ParamWrapper commonParams = spELRuntime.getCommonParams();
        commonParams.importPackage(aClass.getPackage().getName());
        return this;
    }

    public SpELConvert addCommonParam(String paramName, Object paramValue) {
        ParamWrapper commonParams = spELRuntime.getCommonParams();
        commonParams.addVariable(paramName, paramValue);
        return this;
    }


    public Object analyze(String spELExpression) {
        TempPair<String[], List<String>> cutPair = StringUtils.regularCut(spELExpression, PATTERN);
        List<String> expressionList = cutPair.getTwo();
        List<Object> expressionValueList = new ArrayList<>(expressionList.size());
        for (String exp : expressionList) {
            exp = exp.substring(2, exp.length() -1).trim();
            expressionValueList.add(spELRuntime.getValueForType(exp));
        }
        if (expressionValueList.size() == 1) {
            return expressionValueList.get(0);
        }
        return StringUtils.misalignedSplice(cutPair.getOne(), expressionValueList.toArray(new Object[0]));
    }

    public Object analyze(String spELExpression, Map<String, Object> variables) {
        TempPair<String[], List<String>> cutPair = StringUtils.regularCut(spELExpression, PATTERN);
        List<String> expressionList = cutPair.getTwo();
        List<Object> expressionValueList = new ArrayList<>(expressionList.size());
        for (String exp : expressionList) {
            exp = exp.substring(2, exp.length() -1).trim();
            expressionValueList.add(spELRuntime.getValueForType(new ParamWrapper(exp).setVariables(variables)));
        }
        if (expressionValueList.size() == 1) {
            return expressionValueList.get(0);
        }
        return StringUtils.misalignedSplice(cutPair.getOne(), expressionValueList.toArray(new Object[0]));
    }
}
