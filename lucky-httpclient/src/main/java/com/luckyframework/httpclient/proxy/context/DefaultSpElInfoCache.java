package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.spel.SpELVar;
import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import com.luckyframework.httpclient.proxy.spel.SpELConvert;
import com.luckyframework.spel.ParamWrapper;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultSpElInfoCache implements SpElInfoCache {


    private final AtomicBoolean isInit = new AtomicBoolean(false);

    private ParamWrapper paramWrapper;


    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Context context, ContextParamWrapper cpw) {
        if (isInit.compareAndSet(false, true)) {
            paramWrapper = new ParamWrapper(getImportCompletedParamWrapper(context));
            paramWrapper.setRootObject(new HashMap<>());
            extractSpELVal(context, cpw);
        } else {
            cpw.extractPackages(paramWrapper.getKnownPackagePrefixes());
            cpw.extractRootMap(((Map<String, Object>) paramWrapper.getRootObject()));
            cpw.extractVariableMap(paramWrapper.getVariables());
        }
    }

    private ParamWrapper getImportCompletedParamWrapper(Context context) {
        ParamWrapper paramWrapper = new ParamWrapper();
        List<AnnotatedElement> annotatedElements = new LinkedList<>();
        String thisClassPackage = null;
        Context tempContext = context;
        while (tempContext != null) {
            if (tempContext instanceof ClassContext) {
                thisClassPackage = ((ClassContext) tempContext).getCurrentAnnotatedElement().getPackage().getName();
            }
            annotatedElements.add(0, tempContext.getCurrentAnnotatedElement());
            tempContext = tempContext.getParentContext();
        }
        paramWrapper.importPackage(annotatedElements);
        if (thisClassPackage != null) {
            paramWrapper.importPackage(thisClassPackage);
        }
        return paramWrapper;
    }

    private void extractSpELVal(Context context, ContextParamWrapper cpw) {
        List<Context> contextList = new ArrayList<>();
        Context tempContext = context;
        while (tempContext != null) {
            contextList.add(tempContext);
            tempContext = tempContext.getParentContext();
        }

        for (int i = contextList.size() - 1; i >= 0; i--) {
            doExtractSpELVal(contextList.get(i), cpw);
        }
    }

    private void doExtractSpELVal(Context context, ContextParamWrapper cpw) {
        if (context == null) {
            return;
        }
        SpELVar spELVarAnn = context.getMergedAnnotation(SpELVar.class);
        if (spELVarAnn == null) {
            return;
        }

        SpELConvert spELConvert = getSpELConvert(context);

        for (String rootExp : spELVarAnn.root()) {
            TempPair<String, Object> pair = analyticExpression(spELConvert, cpw, rootExp);
            cpw.extractRootKeyValue(pair.getOne(), pair.getTwo());
            ((Map<String, Object>) paramWrapper.getRootObject()).put(pair.getOne(), pair.getTwo());
        }

        for (String valExp : spELVarAnn.var()) {
            TempPair<String, Object> pair = analyticExpression(spELConvert, cpw, valExp);
            cpw.extractVariableKeyValue(pair.getOne(), pair.getTwo());
            paramWrapper.addVariable(pair.getOne(), pair.getTwo());
        }
    }

    private TempPair<String, Object> analyticExpression(SpELConvert spELConverter, ContextParamWrapper cpw, String expression) {
        int index = expression.indexOf("=");
        if (index == -1) {
            throw new IllegalArgumentException("Wrong @SpELVar expression: '" + expression + "'");
        }
        String nameExpression = expression.substring(0, index).trim();
        String valueExpression = expression.substring(index + 1).trim();

        ParamWrapper namePw = new ParamWrapper(cpw.getParamWrapper()).setExpression(nameExpression).setExpectedResultType(String.class);
        ParamWrapper valuePw = new ParamWrapper(cpw.getParamWrapper()).setExpression(valueExpression).setExpectedResultType(Object.class);

        return TempPair.of(spELConverter.parseExpression(namePw), spELConverter.parseExpression(valuePw));

    }

    private SpELConvert getSpELConvert(Context context) {
        return context.getHttpProxyFactory().getSpELConverter();
    }
}
