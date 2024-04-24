//package com.luckyframework.httpclient.proxy.context;
//
//import com.luckyframework.common.TempPair;
//import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
//import com.luckyframework.httpclient.proxy.spel.SpELConvert;
//import com.luckyframework.httpclient.proxy.spel.SpELVar;
//import com.luckyframework.httpclient.proxy.spel.StaticClassEntry;
//import com.luckyframework.spel.ParamWrapper;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@SuppressWarnings("all")
//public class ContextSpELVarManager implements SpELVarManager {
//
//    private final Context context;
//
//    private ParamWrapper annotationParamWrapper;
//
//    public ContextSpELVarManager(Context context) {
//        this.context = context;
//    }
//
//    @Override
//    public synchronized ParamWrapper getAnnotationParamWrapper(ContextParamWrapper cpw) {
//        if (annotationParamWrapper == null) {
//            annotationParamWrapper = new ParamWrapper(getImportCompletedParamWrapper(context));
//            annotationParamWrapper.setRootObject(new HashMap<>());
//            parseSpELVar(cpw);
//        }
//        return annotationParamWrapper;
//    }
//
//    private ParamWrapper getImportCompletedParamWrapper(Context context) {
//        ParamWrapper paramWrapper = new ParamWrapper();
//        paramWrapper.importPackage(context.getCurrentAnnotatedElement());
//        return paramWrapper;
//    }
//
//    private void parseSpELVar(ContextParamWrapper cpw) {
//        if (context == null) {
//            return;
//        }
//        SpELVar spELVarAnn = context.getMergedAnnotation(SpELVar.class);
//        if (spELVarAnn == null) {
//            return;
//        }
//
//        SpELConvert spELConvert = context.getSpELConvert();
//        for (String rootExp : spELVarAnn.root()) {
//            TempPair<String, Object> pair = analyticExpression(spELConvert, cpw, rootExp);
//            ((Map<String, Object>) annotationParamWrapper.getRootObject()).put(pair.getOne(), pair.getTwo());
//        }
//
//        for (String valExp : spELVarAnn.var()) {
//            TempPair<String, Object> pair = analyticExpression(spELConvert, cpw, valExp);
//            annotationParamWrapper.addVariable(pair.getOne(), pair.getTwo());
//        }
//
//        for (Class<?> fun : spELVarAnn.fun()) {
//            StaticClassEntry classEntry = StaticClassEntry.create(fun);
//            annotationParamWrapper.addVariables(classEntry.getAllStaticMethods());
//        }
//    }
//
//    private TempPair<String, Object> analyticExpression(SpELConvert spELConverter, ContextParamWrapper cpw, String expression) {
//        int index = expression.indexOf("=");
//        if (index == -1) {
//            throw new IllegalArgumentException("Wrong @SpELVar expression: '" + expression + "'");
//        }
//        String nameExpression = expression.substring(0, index).trim();
//        String valueExpression = expression.substring(index + 1).trim();
//
//        ParamWrapper namePw = new ParamWrapper(cpw.getParamWrapper()).setExpression(nameExpression).setExpectedResultType(String.class);
//        ParamWrapper valuePw = new ParamWrapper(cpw.getParamWrapper()).setExpression(valueExpression).setExpectedResultType(Object.class);
//
//        return TempPair.of(spELConverter.parseExpression(namePw), spELConverter.parseExpression(valuePw));
//
//    }
//}
