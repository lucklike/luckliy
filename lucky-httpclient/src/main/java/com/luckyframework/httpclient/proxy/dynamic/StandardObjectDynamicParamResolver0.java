//package com.luckyframework.httpclient.proxy.dynamic;
//
//import com.luckyframework.httpclient.proxy.annotations.StandardObjectParam0;
//import com.luckyframework.httpclient.proxy.context.Context;
//import com.luckyframework.httpclient.proxy.context.ValueContext;
//import com.luckyframework.httpclient.proxy.paraminfo.CarrySetterParamInfo;
//import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
//import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
///**
// * 标准的Object动态参数解析器
// *
// * @author fukang
// * @version 1.0.0
// * @date 2023/10/1 07:20
// */
//public class StandardObjectDynamicParamResolver0 extends AbstractDynamicParamResolver {
//
//    private Function<Context, ParameterSetter> defaultSetterFunction;
//    private Function<Context, SmartDynamicParamResolver> defaultSmartDynamicParamResolver;
//    private StandardObjectParam0 standardObjectParam;
//    private ValueContext topValueContext;
//    private DynamicParamContext topDynamicParamContext;
//
//    @Override
//    protected List<? extends ParamInfo> doParser(DynamicParamContext context) {
//        init(context);
//        return parserObject(context, defaultSetterFunction, defaultSmartDynamicParamResolver);
//    }
//
//    private List<? extends ParamInfo> parserObject(DynamicParamContext context, Function<Context, ParameterSetter> setterFunction, Function<Context, SmartDynamicParamResolver> smartDynamicParamResolver) {
//        ValueContext currValueContext = context.getContext();
//        if (canResolve(currValueContext, smartDynamicParamResolver)) {
//            return dynamicParamResolver(context, setterFunction, smartDynamicParamResolver);
//        }
//
//        if (currValueContext.isMapInstance()) {
//
//        }
//
//    }
//
//
//    /**
//     * 初始化
//     */
//    private void init(DynamicParamContext context) {
//        this.topDynamicParamContext = context;
//        this.topValueContext = context.getContext();
//        this.standardObjectParam = context.getMergedAnnotationCheckParent(StandardObjectParam0.class);
//        this.defaultSmartDynamicParamResolver = mc -> mc.generateObject(standardObjectParam.baseResolver());
//        this.defaultSetterFunction = mc -> mc.generateObject(standardObjectParam.setter());
//    }
//
//
//    private List<? extends ParamInfo> dynamicParamResolver(DynamicParamContext context, Function<Context, ParameterSetter> setterFunction, Function<Context, SmartDynamicParamResolver> smartDynamicParamResolverFunction) {
//        ValueContext valueContext = context.getContext();
//        SmartDynamicParamResolver smartDynamicParamResolver = smartDynamicParamResolverFunction.apply(valueContext);
//        ParameterSetter parameterSetter = setterFunction.apply(valueContext);
//        return smartDynamicParamResolver.parser(context)
//                .stream()
//                .map(p -> new CarrySetterParamInfo(p, parameterSetter))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * 判断某个处理器是否能处理当前值
//     *
//     * @param valueContext                      值上下文
//     * @param smartDynamicParamResolverFunction 智能处理器生成函数
//     * @return 是否可以处理
//     */
//    private boolean canResolve(ValueContext valueContext, Function<Context, SmartDynamicParamResolver> smartDynamicParamResolverFunction) {
//        if (smartDynamicParamResolverFunction == null) {
//            return false;
//        }
//
//        SmartDynamicParamResolver smartDynamicParamResolver = smartDynamicParamResolverFunction.apply(valueContext);
//        if (smartDynamicParamResolver == null) {
//            return false;
//        }
//
//        return smartDynamicParamResolver.canResolve(valueContext);
//    }
//
//    private ParameterSetter getDefaultSetterFunction(Context context) {
//        return defaultSetterFunction.apply(context);
//    }
//
//    private SmartDynamicParamResolver getDefaultSmartDynamicParamResolver(Context context) {
//        return defaultSmartDynamicParamResolver.apply(context);
//    }
//
//}
